// Takes input video and mask as input, applies a virtual background using alpha
// blending

#include "absl/strings/str_cat.h"
#include "mediapipe/framework/calculator_framework.h"
#include "mediapipe/framework/deps/file_path.h"
#include "mediapipe/framework/formats/image_frame.h"
#include "mediapipe/framework/formats/image_frame_opencv.h"
#include "mediapipe/framework/formats/matrix.h"
#include "mediapipe/framework/port/opencv_core_inc.h"
#include "mediapipe/framework/port/opencv_imgcodecs_inc.h"
#include "mediapipe/framework/port/opencv_imgproc_inc.h"
#include "mediapipe/framework/port/ret_check.h"
#include "mediapipe/framework/port/status.h"
#include "mediapipe/gpu/gl_calculator_helper.h"
#include "mediapipe/gpu/gl_simple_shaders.h"
#include "mediapipe/gpu/shader_util.h"
#include "mediapipe/util/resource_util.h"
#include <iostream>
#include <vector>

using namespace std;
using namespace cv;

// calculator BackgroundMaskingCalculator
namespace mediapipe {
enum { ATTRIB_VERTEX, ATTRIB_TEXTURE_POSITION, NUM_ATTRIBUTES };

constexpr char kGpuBufferTag[] = "IMAGE_GPU";
constexpr char kMaskGpuTag[] = "MASK_GPU";

class BackgroundMaskingCalculator : public CalculatorBase {
public:
  BackgroundMaskingCalculator() = default;
  ~BackgroundMaskingCalculator() override = default;
  // BackgroundMaskingCalculator() : initialized_(false){}

  static absl::Status GetContract(CalculatorContract *cc);

  absl::Status Open(CalculatorContext *cc) override;
  absl::Status Process(CalculatorContext *cc) override;
  absl::Status Close(CalculatorContext *cc) override;

private:
  absl::Status RenderGpu(CalculatorContext *cc);
  absl::Status InitGpu(CalculatorContext *cc);
  void GlRender();
  bool initialized_ = false;
  mediapipe::GlCalculatorHelper gpu_helper_;
  GLuint program_ = 0;
  cv::Mat background;
};

REGISTER_CALCULATOR(BackgroundMaskingCalculator);
absl::Status BackgroundMaskingCalculator::GetContract(CalculatorContract *cc) {

  cc->Inputs().Tag("IMAGE_GPU").Set<mediapipe::GpuBuffer>();
  cc->Inputs().Tag("MASK_GPU").Set<mediapipe::GpuBuffer>();
  cc->Inputs().Tag("IMG_PATH").Set<mediapipe::ImageFrame>();
  cc->Outputs().Tag("IMAGE_GPU").Set<mediapipe::GpuBuffer>();
  MP_RETURN_IF_ERROR(mediapipe::GlCalculatorHelper::UpdateContract(cc));
  return absl::OkStatus();
}

absl::Status BackgroundMaskingCalculator::Open(CalculatorContext *cc) {
  cc->SetOffset(TimestampDiff(0));

  MP_RETURN_IF_ERROR(gpu_helper_.Open(cc));

  return absl::OkStatus();
}

absl::Status BackgroundMaskingCalculator::Process(CalculatorContext *cc) {
  MP_RETURN_IF_ERROR(gpu_helper_.RunInGlContext([this, &cc]() -> absl::Status {
    if (!initialized_) {
      MP_RETURN_IF_ERROR(InitGpu(cc));
      initialized_ = true;
    }
    MP_RETURN_IF_ERROR(RenderGpu(cc));
    return absl::OkStatus();
    ;
  }));

  return absl::OkStatus();
}

// after defining calculator class, we need to register it with a macro
// invocation REGISTER_CALCULATOR(calculator_class_name).
// REGISTER_CALCULATOR(::mediapipe::BackgroundMaskingCalculator);

absl::Status BackgroundMaskingCalculator::RenderGpu(CalculatorContext *cc) {
  if (cc->Inputs().Tag(kMaskGpuTag).IsEmpty()) {
    cc->Outputs()
        .Tag(kGpuBufferTag)
        .AddPacket(cc->Inputs().Tag(kGpuBufferTag).Value());
    return absl::OkStatus();
  }

  // Get inputs and setup output.
  const Packet &input_packet = cc->Inputs().Tag(kGpuBufferTag).Value();
  const Packet &mask_packet = cc->Inputs().Tag(kMaskGpuTag).Value();

  const auto &input_buffer = input_packet.Get<mediapipe::GpuBuffer>();
  const auto &mask_buffer = mask_packet.Get<mediapipe::GpuBuffer>();

  const Packet &path_packet = cc->Inputs().Tag("IMG_PATH").Value();
  const mediapipe::ImageFrame &input_path =
      path_packet.Get<mediapipe::ImageFrame>();

  auto img_tex = gpu_helper_.CreateSourceTexture(input_buffer);
  auto mask_tex = gpu_helper_.CreateSourceTexture(mask_buffer);
  auto back_tex = gpu_helper_.CreateSourceTexture(input_buffer);
  if (input_path.Height() > 1) {
    back_tex = gpu_helper_.CreateSourceTexture(input_path);
  }
  auto dst_tex =
      gpu_helper_.CreateDestinationTexture(img_tex.width(), img_tex.height());

  // Run recolor shader on GPU.

  gpu_helper_.BindFramebuffer(dst_tex);

  glActiveTexture(GL_TEXTURE1);
  glBindTexture(img_tex.target(), img_tex.name());
  glActiveTexture(GL_TEXTURE2);
  glBindTexture(mask_tex.target(), mask_tex.name());
  glActiveTexture(GL_TEXTURE3);
  glBindTexture(back_tex.target(), back_tex.name());

  GlRender();

  glActiveTexture(GL_TEXTURE3);
  glBindTexture(GL_TEXTURE_2D, 0);
  glActiveTexture(GL_TEXTURE2);
  glBindTexture(GL_TEXTURE_2D, 0);
  glActiveTexture(GL_TEXTURE1);
  glBindTexture(GL_TEXTURE_2D, 0);
  glFlush();

  // Send result image in GPU packet.
  auto output = dst_tex.GetFrame<mediapipe::GpuBuffer>();
  cc->Outputs().Tag(kGpuBufferTag).Add(output.release(), cc->InputTimestamp());

  // Cleanup
  img_tex.Release();
  mask_tex.Release();
  back_tex.Release();
  dst_tex.Release();

  return absl::OkStatus();
}

void BackgroundMaskingCalculator::GlRender() {
  static const GLfloat square_vertices[] = {
      -1.0f, -1.0f, // bottom left
      1.0f,  -1.0f, // bottom right
      -1.0f, 1.0f,  // top left
      1.0f,  1.0f,  // top right
  };
  static const GLfloat texture_vertices[] = {
      0.0f, 0.0f, // bottom left
      1.0f, 0.0f, // bottom right
      0.0f, 1.0f, // top left
      1.0f, 1.0f, // top right
  };

  // program
  glUseProgram(program_);

  // vertex storage
  GLuint vbo[2];
  glGenBuffers(2, vbo);
  GLuint vao;
  glGenVertexArrays(1, &vao);
  glBindVertexArray(vao);

  // vbo 0
  glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
  glBufferData(GL_ARRAY_BUFFER, 4 * 2 * sizeof(GLfloat), square_vertices,
               GL_STATIC_DRAW);
  glEnableVertexAttribArray(ATTRIB_VERTEX);
  glVertexAttribPointer(ATTRIB_VERTEX, 2, GL_FLOAT, 0, 0, nullptr);

  // vbo 1
  glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
  glBufferData(GL_ARRAY_BUFFER, 4 * 2 * sizeof(GLfloat), texture_vertices,
               GL_STATIC_DRAW);
  glEnableVertexAttribArray(ATTRIB_TEXTURE_POSITION);
  glVertexAttribPointer(ATTRIB_TEXTURE_POSITION, 2, GL_FLOAT, 0, 0, nullptr);

  // draw
  glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

  // cleanup
  glDisableVertexAttribArray(ATTRIB_VERTEX);
  glDisableVertexAttribArray(ATTRIB_TEXTURE_POSITION);
  glBindBuffer(GL_ARRAY_BUFFER, 0);
  glBindVertexArray(0);
  glDeleteVertexArrays(1, &vao);
  glDeleteBuffers(2, vbo);
}

absl::Status BackgroundMaskingCalculator::Close(CalculatorContext *cc) {
  gpu_helper_.RunInGlContext([this] {
    if (program_)
      glDeleteProgram(program_);
    program_ = 0;
  });

  return absl::OkStatus();
  ;
}

absl::Status BackgroundMaskingCalculator::InitGpu(CalculatorContext *cc) {

  const GLint attr_location[NUM_ATTRIBUTES] = {
      ATTRIB_VERTEX,
      ATTRIB_TEXTURE_POSITION,
  };
  const GLchar *attr_name[NUM_ATTRIBUTES] = {
      "position",
      "texture_coordinate",
  };

  std::string mask_component;
  mask_component = "a";

  // A shader to blend a color onto an image where the mask > 0.
  // The blending is based on the input image luminosity.
  const std::string frag_src = R"(
  #if __VERSION__ < 130
    #define in varying
  #endif  // __VERSION__ < 130

  #ifdef GL_ES
    #define fragColor gl_FragColor
    precision highp float;
  #else
    #define lowp
    #define mediump
    #define highp
    #define texture2D texture
    out vec4 fragColor;
  #endif  // defined(GL_ES)

    #define MASK_COMPONENT )" + mask_component +
                               R"(

  in vec2 sample_coordinate;
 	uniform sampler2D frame;
 	uniform sampler2D mask;
 	uniform sampler2D background;
 	uniform float invert_mask;

 	void main()
 	{
 		vec4 weight = texture2D(mask, sample_coordinate);
 		vec4 foreground = texture2D(frame, sample_coordinate);
 		vec4 back = texture2D(background, sample_coordinate);

 		weight = mix(weight, 1.0 - weight, invert_mask);

 		float mix_value = weight.MASK_COMPONENT;

    // mix_value = mix_value > 0.5 ? 1.0 : 0.0;

 		fragColor = mix(foreground, back, mix_value);
 	}
)";

  // shader program and params
  mediapipe::GlhCreateProgram(mediapipe::kBasicVertexShader, frag_src.c_str(),
                              NUM_ATTRIBUTES, &attr_name[0], attr_location,
                              &program_);
  RET_CHECK(program_) << "Problem initializing the program.";
  glUseProgram(program_);
  glUniform1i(glGetUniformLocation(program_, "frame"), 1);
  glUniform1i(glGetUniformLocation(program_, "mask"), 2);
  glUniform1i(glGetUniformLocation(program_, "background"), 3);
  glUniform1f(glGetUniformLocation(program_, "invert_mask"), 1);

  return absl::OkStatus();
}
} // namespace mediapipe

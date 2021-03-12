package graphics.scenery.spirvcrossj.utils;

import graphics.scenery.spirvcrossj.base.CompilerGLSL;
import graphics.scenery.spirvcrossj.base.IntVec;
import graphics.scenery.spirvcrossj.base.ShaderResources;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <Description>
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
public class TestVulkanToGLSL
{
  static {
    Loader.INSTANCE.loadNatives();
  }

  private void spirv_add(IntVec spirv, IntBuffer ib)
  {
    int pos = ib.position();
    long value = ib.get();
    spirv.set(pos, value);
  }

  @Test
  public void convertVulkanToGLSL310() throws IOException, URISyntaxException
  {
    ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(
      Paths.get(
        this.getClass().getClassLoader().getResource("fullscreen-quad.spv")
            .toURI())));
    IntBuffer ib = data.asIntBuffer();
    IntVec spirv = new IntVec(ib.capacity());

    while (ib.hasRemaining()) {
      spirv_add(spirv, ib);
    }

    System.out.println("Read " + ib.position() + " opcodes from SPIR-V binary.\n");

    CompilerGLSL compiler = new CompilerGLSL(spirv);
    CompilerGLSL.Options options = new CompilerGLSL.Options();
    options.setVersion(310);
    options.setEs(false);

    compiler.setCommonOptions(options);

    // output GLSL 3.10 code
    System.out.println("SPIR-V converted to GLSL 3.10:\n\n" + compiler.compile());
  }

  @Test
  public void listUniformBuffers() throws IOException, URISyntaxException
  {
    InputStream is = this.getClass().getClassLoader()
                         .getResourceAsStream("spvFileList.txt");

    if (is == null) {
      throw new RuntimeException("Failed to read spvFileList.txt");
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(is));
    List<String> spvFileList = in.lines().collect(Collectors.toList());
    in.close();

    for (String filename : spvFileList) {
      ByteBuffer data = ByteBuffer.wrap(
        Files.readAllBytes(
          Paths.get(this.getClass()
                        .getClassLoader()
                        .getResource(
                          filename)
                        .toURI())));
      IntBuffer ib = data.asIntBuffer();
      IntVec spirv = new IntVec(ib.capacity());

      while (ib.hasRemaining()) {
        spirv_add(spirv, ib);
      }

      System.out.println("Read "
                         + ib.position()
                         + " opcodes from SPIR-V binary "
                         + filename
                         + ".\n");

      CompilerGLSL compiler = new CompilerGLSL(spirv);

      ShaderResources res = compiler.getShaderResources();
      for (int i = 0; i < res.getUniformBuffers().capacity(); i++) {
        System.err.println(compiler.getType(res.getUniformBuffers().get(i).getTypeId())
                                   .getBasetype() + ": " + res.getUniformBuffers().get(i)
                                                              .getName());
      }

      for (int i = 0; i < res.getSampledImages().capacity(); i++) {
        System.err.println(compiler.getType(res.getSampledImages().get(i).getTypeId())
                                   .getBasetype() + ": " + res.getSampledImages().get(i)
                                                              .getName());
      }
    }
  }

  @Test(expected = RuntimeException.class)
  public void checkExceptionHandling() throws IOException, URISyntaxException
  {
    InputStream is = this.getClass().getClassLoader()
                         .getResourceAsStream("spvFileList.txt");

    if (is == null) {
      throw new RuntimeException("Failed to read spvFileList.txt");
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(is));
    List<String> spvFileList = in.lines().collect(Collectors.toList());
    in.close();

    for (String filename : spvFileList) {
      ByteBuffer data = ByteBuffer.wrap(
        Files.readAllBytes(
          Paths.get(
            this.getClass().getClassLoader().getResource(filename).toURI())));
      IntBuffer ib = data.asIntBuffer();
      IntVec spirv = new IntVec(ib.capacity());

      while (ib.hasRemaining()) {
        spirv_add(spirv, ib);
      }

      CompilerGLSL compiler = new CompilerGLSL(spirv);
      ShaderResources res = compiler.getShaderResources();

      for (int i = 0; i < res.getUniformBuffers().capacity(); i++) {
        long id = res.getUniformBuffers().get(i).getId();
        compiler.getType(id);

        // junit 5+
        //Assertions.assertThrows(
        //  RuntimeException.class,
        //  () -> {
        //    // expected to throw a CompilerError
        //    compiler.getType(id);
        //  },
        //  res.getUniformBuffers().get(i).getId()
        //  + " should have produced a compiler error!");
      }
    }
  }
}

package graphics.scenery.spirvcrossj.utils;

import graphics.scenery.spirvcrossj.base.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <Description>
 *
 * @author Ulrik G?nther <hello@ulrik.is>
 */
public class TestGLSLToVulkan
{
  static {
    Loader.INSTANCE.loadNatives();
  }

  @Test
  public void convertGLSLToVulkan()
    throws IOException, URISyntaxException, InterruptedException
  {
    //Loader.loadNatives();

    InputStream is = this.getClass().getClassLoader()
                         .getResourceAsStream("shaderFileList.txt");

    if (is == null) {
      throw new RuntimeException("Failed to read shaderFileList.txt");
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(is));
    List<String> spvFileList = in.lines().collect(Collectors.toList());
    in.close();

    if (!LibSPIRVCrossJ.initializeProcess()) {
      throw new RuntimeException("glslang failed to initialize.");
    }

    final SWIGTYPE_p_TBuiltInResource resources = LibSPIRVCrossJ
      .getDefaultTBuiltInResource();

    for (String filename : spvFileList) {

      Boolean compileFail = false;
      Boolean linkFail = false;
      final TProgram program = new TProgram();
      final String[] names = {filename};

      final String code;
      BufferedReader spvReader = new BufferedReader(new InputStreamReader(this
                                                                            .getClass()
                                                                            .getClassLoader()
                                                                            .getResourceAsStream(
                                                                              filename)));
      code = spvReader.lines().collect(Collectors.joining("\n"));
      spvReader.close();

      final String dummyShader[] = {
        code
      };

      final String extension = filename.substring(filename.lastIndexOf('.') + 1);
      int shaderType = 0;

      switch (extension) {
        case "vert":
          shaderType = EShLanguage.EShLangVertex;
          break;
        case "frag":
          shaderType = EShLanguage.EShLangFragment;
          break;
        case "geom":
          shaderType = EShLanguage.EShLangGeometry;
          break;
        case "tesc":
          shaderType = EShLanguage.EShLangTessControl;
          break;
        case "tese":
          shaderType = EShLanguage.EShLangTessEvaluation;
          break;
        case "comp":
          shaderType = EShLanguage.EShLangCompute;
          break;
        case "txt":
          continue;
        default:
          throw new RuntimeException("Unknown shader extension ." + extension);
      }

      final TShader shader = new TShader(shaderType);

      System.out.println(filename
                         + ": Compiling shader code  ("
                         + dummyShader[0].length()
                         + " bytes)... ");

      final Boolean shouldFail = code.contains("ERROR");
      if (shouldFail) {
        System.out.println("This file is expected not to compile successfully.");
      }

      shader.setStrings(dummyShader, 1);

      shader.setAutoMapBindings(true);

      int messages = EShMessages.EShMsgDefault;
      messages |= EShMessages.EShMsgVulkanRules;
      messages |= EShMessages.EShMsgSpvRules;

      if (!shader.parse(resources, 450, false, messages)) {
        compileFail = true;
      }

      if (compileFail && !shouldFail) {
        System.out.println("Info log: " + shader.getInfoLog());
        System.out.println("Debug log: " + shader.getInfoDebugLog());
        throw new RuntimeException("Compilation of " + filename + " failed");
      }

      if (compileFail && shouldFail) {
        System.out.println("Linking skipped as compilation was expected to fail...");
        continue;
      }

      program.addShader(shader);

      if (!program.link(EShMessages.EShMsgDefault)) {
        linkFail = true;
      }

      if (!program.mapIO()) {
        linkFail = true;
      }


      if (linkFail && !shouldFail) {
        System.err.println(program.getInfoLog());
        System.err.println(program.getInfoDebugLog());

        throw new RuntimeException("Linking of program " + filename + " failed!");
      }

      if (!linkFail && !compileFail && !shouldFail) {
        final IntVec spirv = new IntVec();
        LibSPIRVCrossJ.glslangToSpv(program.getIntermediate(shaderType), spirv);

        System.out.println("Generated " + spirv.capacity() + " bytes of SPIRV bytecode.");
//        assert(spirv.capacity() % 4 == 0);

        //System.out.println(shader);
        //System.out.println(program);
      }

    }
    LibSPIRVCrossJ.finalizeProcess();
  }
}

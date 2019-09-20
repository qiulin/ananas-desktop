package org.ananas.runner.steprunner.subprocess;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.ananas.runner.steprunner.subprocess.utils.CallingSubProcessUtils;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the process kernel which deals with exec of the subprocess. It also deals with all I/O.
 */
public class SubProcessKernel {

  private static final Logger LOG = LoggerFactory.getLogger(SubProcessKernel.class);

  private static final int MAX_SIZE_COMMAND_LINE_ARGS = 128 * 1024;

  SubProcessConfiguration configuration;
  ProcessBuilder processBuilder;
  Schema outputSchema;

  private SubProcessKernel() {}

  /**
   * Creates the SubProcess Kernel ready for execution. Will deal with all input and outputs to the
   * SubProcess
   *
   * @param options
   */
  public SubProcessKernel(SubProcessConfiguration options, Schema outputSchema) {
    this.configuration = options;
    this.processBuilder =
        new ProcessBuilder(
            configuration.binaryName, configuration.workerPath + configuration.executableName);
    this.outputSchema = outputSchema;
  }

  /**
   * Execute binary
   *
   * @param b the outputstream to be written
   * @return
   * @throws Exception
   */
  public List<GenericRecord> exec(ByteArrayOutputStream b) throws Exception {
    try (CallingSubProcessUtils.Permit permit =
        new CallingSubProcessUtils.Permit(configuration.executableName)) {
      try {
        Process process = execBinary(processBuilder, b);
        return collectProcessResults(process, processBuilder);
      } catch (Exception ex) {
        LOG.error("Error running executable ", ex);
        throw ex;
      }
    }
  }

  /**
   * Execute binary .
   *
   * @param builder Process builder
   * @param out the outputstream to be written to STDIN
   * @return a built process
   * @throws Exception
   */
  private Process execBinary(ProcessBuilder builder, ByteArrayOutputStream out) throws Exception {
    try {
      Process process = builder.start();

      // Write to STDIN
      try (OutputStream o = process.getOutputStream()) {
        out.writeTo(o);
        out.close();
      }

      boolean timeout = !process.waitFor(configuration.getWaitTime(), TimeUnit.SECONDS);

      if (timeout) {
        String log =
            String.format(
                "Timeout waiting to run process with parameters %s . "
                    + "Check to see if your timeout is long enough. Currently set at %s.",
                createLogEntryFromInputs(builder.command()), configuration.getWaitTime());
        throw new Exception(log);
      }
      return process;

    } catch (Exception ex) {

      LOG.error(
          String.format(
              "Error running process with parameters %s error was %s ",
              createLogEntryFromInputs(builder.command()), ex.getMessage()));
      throw new Exception(ex);
    }
  }

  /**
   * Collect process results
   *
   * @param process
   * @param builder
   * @return List of results
   * @throws Exception if process has non 0 value or no logs found then throw exception
   */
  private List<GenericRecord> collectProcessResults(Process process, ProcessBuilder builder)
      throws Exception {

    List<GenericRecord> results = new ArrayList<>();

    try {
      // If process exit value is not 0 then subprocess failed, record logs
      if (process.exitValue() != 0) {

        StringBuilder logBuilder = new StringBuilder();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

          while (reader.ready()) logBuilder.append(reader.readLine());
        } catch (Exception e) {
          throw e;
        }

        throw new Exception(
            "exit code is " + process.exitValue() + ". STDERR: " + logBuilder.toString());
      }

      // Everything looks healthy return values
      GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<>(outputSchema);
      try (DataFileReader<GenericRecord> r =
          new DataFileReader<GenericRecord>(
              new SeekableByteArrayInput(IOUtils.toByteArray(process.getInputStream())),
              datumReader)) {
        while (r.hasNext()) results.add(r.next());
      } catch (Exception e) {
        e.printStackTrace();
      }

      return results;
    } catch (Exception ex) {
      String log =
          String.format(
              "Unexpected error running process. %s error message was %s",
              createLogEntryFromInputs(builder.command()), ex.getMessage());
      System.err.println(log);
      throw new Exception(log);
    }
  }

  private static String createLogEntryFromInputs(List<String> commands) {
    String params;
    if (commands != null) {
      params = String.join(",", commands);
    } else {
      params = "No-Commands";
    }
    return params;
  }
}
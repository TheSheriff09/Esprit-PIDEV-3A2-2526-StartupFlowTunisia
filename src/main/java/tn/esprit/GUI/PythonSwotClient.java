package tn.esprit.GUI;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PythonSwotClient {

    public static String run(String pythonExe, String scriptPath, String jsonInput) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (OutputStream os = p.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line).append("\n");
        }

        int code = p.waitFor();
        String result = out.toString().trim();

        if (result.startsWith("ERROR:")) throw new RuntimeException(result);
        if (code != 0) throw new RuntimeException("Python exit code=" + code + "\n" + result);

        return result;
    }
}

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server{
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png");
    private  final ExecutorService executorService;

    public Server(int threadPoolSize){
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void addValidPath(List<String> validPaths, String path){
        validPaths.add(path);
    }

    public void deleteValidPath(List<String> validPaths, String path){
        validPaths.remove(validPaths.indexOf(path));
    }

    public void listen(int port){
        try (final ServerSocket serverSocket = new ServerSocket(port)){
            while (true){
                final Socket socket = serverSocket.accept();
                executorService.submit(() -> process(socket));
                process(socket);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void process(Socket socket){
        try(
                socket;
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                ) {

            final String requestLine = in.readLine();
            final String[] parts = requestLine.split(" ");

            if (parts.length != 3) {
                return;
            }

            final String path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }
            final Path filePath = Path.of(".", "public", path);
            final String mimeType = Files.probeContentType(filePath);
            final long length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
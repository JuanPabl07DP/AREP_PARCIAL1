package co.edu.escuelaing.arep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

/**
 * @author Juan Pablo Daza Pereira
 */
public class ReflectiveChatGPT {

    public static void main(String[] args) {
        try {

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(45000);
            } catch (IOException e) {
                System.err.println("Could not listen on port:" + e.getMessage());
                System.exit(1);
            }

            boolean running = true;
            while (running) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    System.err.println("Accept failed " + e.getMessage());
                    System.exit(1);
                }
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine, outputLine;

                boolean firstLine = true;
                String requestStringURI = "";

                while ((inputLine = in.readLine()) != null) {
                    if (firstLine) {
                        System.out.println("Received: " + inputLine);
                        requestStringURI = inputLine.split(" ")[1];
                        firstLine = false;
                        continue;
                    }
                    if (!in.ready()) {
                        break;
                    }
                }

                URI requestURI = new URI(requestStringURI);
                System.out.println(requestURI.toString());
                JsonObject response = makeReflection(requestURI.getQuery().split("=")[1]);
                outputLine = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n" +
                        response.toString();
                out.println(outputLine);
                out.close();
                in.close();
                clientSocket.close();
            }
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject makeReflection(String query) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException {
        JsonObject response = new JsonObject();
        String reflectiveMethod = query.split("\\(")[0];
        String params = query.split("\\(")[1].split("\\)")[0];
        String[] paramsArray = params.split(",");
        Class<?> classes = null;
        String stringMethodName = "";

        classes = Class.forName(paramsArray[0]);
        if (paramsArray.length > 1) {
            stringMethodName = paramsArray[1];
        }

        if (reflectiveMethod.equals("class")) {
            response = getDeclaredFieldsAndMethods(classes);
        } else if (reflectiveMethod.equals("invoke")) {
            Class<?>[] offeredTypesArray = {};
            Method method = classes.getMethod(stringMethodName, offeredTypesArray);
            response = invokingMethod(method);
        }
        return response;
    }

    private static JsonObject invokingMethod(Method method) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        JsonObject response = new JsonObject();
        Object result = method.invoke(null, null);
        response.addProperty("result", result.toString());
        return response;
    }

    private static JsonObject getDeclaredFieldsAndMethods(Class<?> classes) {
        JsonObject response = new JsonObject();
        Method[] methods = classes.getDeclaredMethods();
        Field[] fields = classes.getDeclaredFields();
        List<String> methodsList = new ArrayList<>();
        List<String> fieldsList = new ArrayList<>();
        for (Method method : methods) {
            methodsList.add(method.getName());
        }
        for (Field field : fields) {
            fieldsList.add(field.getName());
        }
        response.addProperty("methodsList", methodsList.toString());
        response.addProperty("fieldsList", fieldsList.toString());
        return response;
    }

}

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;

public class Shader {
    // program ID
    private int ID;
    private FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    String ParseShader(String shaderPath) {
        StringBuilder vertexCode = new StringBuilder();
        try {
            BufferedReader vertexReader = new BufferedReader(new FileReader(shaderPath));
            String line;
            while ((line = vertexReader.readLine()) != null) {
                vertexCode.append(line).append("//\n");
            }
            vertexReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return vertexCode.toString();
    }

    int CompileShader(int type, String source) {
        int id;
        id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);
        // check for shader compile errors
        int success[] = new int[1];
        GL20.glGetShaderiv(id, GL20.GL_COMPILE_STATUS, success);
        if (success[0] == 0) {
            String infoLog = GL20.glGetShaderInfoLog(id, 512);
            System.out.println("ERROR::SHADER::VERTEX::COMPILATION_FAILED\\n" + infoLog + "\n");
        }
        return id;
    }


    // constructor reads and builds the shader
    public Shader(String vertexPath, String fragmentPath) {

        String vertexSource = ParseShader(vertexPath);
        String fragmentSource = ParseShader(fragmentPath);
        int vertexID = CompileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentID = CompileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        ID = GL20.glCreateProgram();
        GL20.glAttachShader(ID, vertexID);
        GL20.glAttachShader(ID, fragmentID);
        GL20.glLinkProgram(ID);

        int success[] = new int[1];
        GL20.glGetProgramiv(ID, GL20.GL_LINK_STATUS, success);
        if (success[0] == 0) {
            String infoLog = GL20.glGetProgramInfoLog(ID, 512);
            System.out.println("ERROR::SHADER::PROGRAM::LINKING_FAILED\n" + infoLog + "\n");
        }
        // delete the shaders as they're linked into our program now and no longer necessary
        GL20.glDeleteShader(vertexID);
        GL20.glDeleteShader(fragmentID);
    }

    // use/activate the shader
    public void use() {
        GL20.glUseProgram(ID);
    }

    // utility uniform functions
    public void setBool(String name, boolean value) {
        int val = value ? 1 : 0;
        GL20.glUniform1i(GL20.glGetUniformLocation(ID, name), val);
    }

    public void setInt(String name, int value) {
        GL20.glUniform1i(GL20.glGetUniformLocation(ID, name), value);
    }

    public void setFloat(String name, float value) {
        GL20.glUniform1f(GL20.glGetUniformLocation(ID, name), value);
    }

    public void setMatrix(String name, Matrix4f value) {
        GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(ID, name), false, value.get(fb));
    }

    public void setVec3(String name, Vector3f value) {
        GL20.glUniform3f(GL20.glGetUniformLocation(ID, name), value.x, value.y, value.z);
    }

    public void setVec3(String name, float x, float y, float z) {
        GL20.glUniform3f(GL20.glGetUniformLocation(ID, name), x, y, z);
    }

    public void dispose() {
        GL20.glDeleteProgram(ID);
    }
}

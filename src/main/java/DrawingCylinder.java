import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class DrawingCylinder {

    public static final int SCREEN_WIDTH = 600;
    public static final int SCREEN_HEIGHT = 600;
    public static final int POSITIONS_SIZE = 3;
    public static final int NORMALS_SIZE = 3;
    public static final int ITERATIONS = POSITIONS_SIZE + NORMALS_SIZE;

    private static final int HORIZONTAL_SEGMENT = 20;
    private static final int SECTORS_COUNT = 50;
    private static final float CYLINDER_RADIUS = 0.3f;

    private final static int FAN_LENGTH = SECTORS_COUNT + 2;
    private final static int STRIP_LENGTH = SECTORS_COUNT * 2 + 2;


    private static int SOUTH_CENTER_VERTEX;
    private static int NORTH_CENTER_VERTEX;

    private static float CAMERA_RADIUS = 4.0f;

    // The window handle
    private long window;

    private int VBO;
    private Shader lightingShader;
    boolean pressed;

    public void run() {
        init();
        loop();

        glDeleteBuffers(VBO);
        lightingShader.dispose();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }
    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        glClearDepth(-1);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);
            glViewport(0, 0, pWidth.get(0), pHeight.get(0));
        } // the stack frame is popped automatically

        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            glViewport(0, 0, width, height);
        });

        lightingShader = new Shader("src/shaders/vertex.shader", "src/shaders/fragment.shader");

        final float[] coordinates = getVertices();
        final int[] indices = getIndices();

        final Matrix4f model = new Matrix4f();
        final Matrix4f projection = new Matrix4f();

        // camera
        final Matrix4f camera = new Matrix4f();
        final Vector3f eye = new Vector3f(0.0f, 2.0f, 3.0f);
        final Vector3f center = new Vector3f(0.0f, 0.5f, 0.0f);
        final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

        int VAO = glGenVertexArrays();
        glBindVertexArray(VAO);

        VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, coordinates, GL_STATIC_DRAW);

        // x y z
        glVertexAttribPointer(0, POSITIONS_SIZE, GL_FLOAT, false, ITERATIONS * 4, 0);
        glEnableVertexAttribArray(0);

        // normals
        glVertexAttribPointer(1, NORMALS_SIZE, GL_FLOAT, false, ITERATIONS * 4, POSITIONS_SIZE * 4);
        glEnableVertexAttribArray(1);

        final int INB = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, INB);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        lightingShader.use();

        GLFW.glfwSetScrollCallback(window, new GLFWScrollCallback() {
            @Override public void invoke (long win, double dx, double dy) {
                CAMERA_RADIUS += dy * 0.5f;
            }
        });

        double angleX = Math.toRadians(45), angleY = Math.toRadians(60);
        final double[] xPos = new double[1];
        final double[] yPos = new double[1];
        final Vector2f lastMousePos = new Vector2f();
        GLFW.glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                pressed = action == 1;
                glfwGetCursorPos(window, xPos, yPos);
                lastMousePos.x = (float) xPos[0];
                lastMousePos.y = (float) yPos[0];
            }
        });

        final Vector3f lightPos = new Vector3f(1.2f, -2.0f, 2.0f);
        lightingShader.setVec3("objectColor", 1.0f, 0.5f, 0.31f);
        lightingShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);
        lightingShader.setVec3("lightPos", lightPos);

        // run the rendering loop until the user has attempted to close the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            // set the view matrix
            projection.identity();
            projection.perspective(Math.toRadians(45.0f), (float) SCREEN_WIDTH / SCREEN_HEIGHT, 0.1f, 100.0f);

            if (pressed) {
                glfwGetCursorPos(window, xPos, yPos);
                double x = lastMousePos.x - xPos[0];
                double y = lastMousePos.y - yPos[0];
                double changedX = x / SCREEN_WIDTH * 2 * Math.PI;
                double changedY = y / SCREEN_HEIGHT * 2 * Math.PI;
                lastMousePos.x = (float) xPos[0];
                lastMousePos.y = (float) yPos[0];

                angleX += changedX;
                angleY += changedY;
            }

            // update camera
            eye.set(
                    (float) (CAMERA_RADIUS * Math.sin(angleY) * Math.cos(angleX)),
                    (float) (CAMERA_RADIUS * Math.cos(angleY)),
                    (float) (CAMERA_RADIUS * Math.sin(angleY) * Math.sin(angleX))
            );

            camera.identity();
            camera.lookAt(eye, center, up);

            // set the clear color
            glClearColor(28 / 255f, 30 / 255f, 38 / 255f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // update shader
            lightingShader.setVec3("viewPos", eye);
            lightingShader.setMatrix("model", model);
            lightingShader.setMatrix("view", camera);
            lightingShader.setMatrix("projection", projection);

            lightingShader.use();

            // draw caps with triangle fan
            glDrawElements(GL_TRIANGLE_FAN, FAN_LENGTH, GL_UNSIGNED_INT, 0);
            glDrawElements(GL_TRIANGLE_FAN, FAN_LENGTH, GL_UNSIGNED_INT, FAN_LENGTH * 4);

            // draw body with triangle strip
            glDrawElements(GL_TRIANGLE_STRIP, HORIZONTAL_SEGMENT * STRIP_LENGTH, GL_UNSIGNED_INT, 2 * FAN_LENGTH * 4);
//            glDrawElements(GL_LINE_STRIP, HORIZONTAL_SEGMENT * STRIP_LENGTH, GL_UNSIGNED_INT, 2 * FAN_LENGTH * 4);

            // swap the color buffers
            glfwSwapBuffers(window);

            // poll for window events. The key callback above will only be invoked during this call.
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new DrawingCylinder().run();
    }

    public float[] getVertices() {
        final int centralPointsCount = 2;
        final int additionalCirclesCount = centralPointsCount;
        final int circlesCount = HORIZONTAL_SEGMENT + 1 + additionalCirclesCount;
        final int circlesPointsCount = circlesCount * SECTORS_COUNT;

        // vertices
        final float[] vertices = new float[(circlesPointsCount + centralPointsCount) * ITERATIONS];

        // populate segments
        for (int segmentIndex = 0; segmentIndex < circlesCount - 2; segmentIndex++) {
            // populate segment circles
            final float[] circle = getSectorVertices(segmentIndex);
            System.arraycopy(circle, 0, vertices, segmentIndex * circle.length, circle.length);
        }

        // set south center point
        float x = 0, y = 0, z = 0;
        SOUTH_CENTER_VERTEX = circlesPointsCount;
        setPoint(vertices, SOUTH_CENTER_VERTEX, x, y, z, 0, -1, 0);
        // set south circle vertices
        final float[] southCircle = getSectorVertices(HORIZONTAL_SEGMENT, 0, -1, 0);
        System.arraycopy(southCircle, 0, vertices, (circlesCount - 2) * southCircle.length, southCircle.length);

        // set north center point
        x = 0; y = 1; z = 0;
        NORTH_CENTER_VERTEX = circlesPointsCount + 1;
        setPoint(vertices, NORTH_CENTER_VERTEX, x, y, z, 0, 1, 0);

        // set north circle vertices
        final float[] northCircle = getSectorVertices(0, 0, 1, 0);
        System.arraycopy(northCircle, 0, vertices, (circlesCount - 1) * northCircle.length, northCircle.length);

        return vertices;
    }
    public float[] getSectorVertices(int sectorIndex) {
        final float[] vertices = new float[SECTORS_COUNT * ITERATIONS];

        // position
        float y = 1 - (float) sectorIndex / HORIZONTAL_SEGMENT;

        // populate surrounding points on a circle
        for (int i = 0; i < SECTORS_COUNT; i++) {
            double angle = (float) i / SECTORS_COUNT * 2 * Math.PI;
            float x = (float) Math.sin(angle);
            float z = (float) Math.cos(angle);
            setPoint(vertices, i, x * CYLINDER_RADIUS, y, z * CYLINDER_RADIUS, x, 0, z);
        }

        return vertices;
    }
    public float[] getSectorVertices(int sectorIndex, float nx, float ny, float nz) {
        final float[] vertices = new float[SECTORS_COUNT * ITERATIONS];

        // position
        float y = 1 - (float) sectorIndex / HORIZONTAL_SEGMENT;

        // populate surrounding points on a circle
        for (int i = 0; i < SECTORS_COUNT; i++) {
            double angle = (float) i / SECTORS_COUNT * 2 * Math.PI;
            float x = (float) Math.sin(angle);
            float z = (float) Math.cos(angle);
            setPoint(vertices, i, x * CYLINDER_RADIUS, y, z * CYLINDER_RADIUS, nx, ny, nz);
        }

        return vertices;
    }

    public int[] getIndices() {
        final int[] indices = new int[2 * FAN_LENGTH + HORIZONTAL_SEGMENT * STRIP_LENGTH];

        int destPos = 0;

        // add south cap indices
        final int[] cap = getCapIndices(SOUTH_CENTER_VERTEX, HORIZONTAL_SEGMENT + 1);
        final int[] southCapIndices = new int[cap.length];
        southCapIndices[0] = cap[0];
        for (int i = 1; i < cap.length; i++) {
            southCapIndices[i] = cap[cap.length - i];
        }
        System.arraycopy(southCapIndices, 0, indices, destPos, southCapIndices.length);
        destPos += southCapIndices.length;

        // add north cap indices
        final int[] northCapIndices = getCapIndices(NORTH_CENTER_VERTEX, HORIZONTAL_SEGMENT + 2);
        System.arraycopy(northCapIndices, 0, indices, destPos, northCapIndices.length);
        destPos += northCapIndices.length;

        // add body
        for (int i = 0; i < HORIZONTAL_SEGMENT; i++) {
            final int[] segmentIndices = getSectorsIndices(i, i + 1);
            System.arraycopy(segmentIndices, 0, indices, destPos, segmentIndices.length);
            destPos += segmentIndices.length;
        }

        return indices;
    }
    public int[] getCapIndices(int centerVertex, int circleIndex) {
        final int[] indices = new int[FAN_LENGTH];

        final int circleStartVertex = circleIndex * SECTORS_COUNT - 1;

        // set first point the center vertex
        indices[0] = centerVertex;

        // add surrounding vertices
        for (int i = 1; i < SECTORS_COUNT + 1; i++) {
            indices[i] = circleStartVertex + i;
        }

        // add degenerate
        indices[SECTORS_COUNT + 1] = circleStartVertex + 1;

        return indices;
    }
    public int[] getSectorsIndices(int firstCircleIndex, int secondCircleIndex) {
        final int[] indices = new int[STRIP_LENGTH];

        final int firstCircleStartVertex = firstCircleIndex * SECTORS_COUNT;
        final int secondCircleStartVertex = secondCircleIndex * SECTORS_COUNT;

        for (int i = 0, index = 0; i < SECTORS_COUNT; i++, index += 2) {
            indices[index] = i + firstCircleStartVertex;
            indices[index + 1] = i + secondCircleStartVertex;
        }
        // add degenerates
        indices[SECTORS_COUNT * 2] = firstCircleStartVertex;
        indices[SECTORS_COUNT * 2 + 1] = secondCircleStartVertex;

        return indices;
    }

    public void setPoint(float[] coordinates, int index, float x, float y, float z, float nx, float ny, float nz) {
        index *= DrawingCylinder.ITERATIONS;

        // set position
        coordinates[index] = x;
        coordinates[index + 1] = y;
        coordinates[index + 2] = z;

        // normals
        coordinates[index + 3] = nx;
        coordinates[index + 4] = ny;
        coordinates[index + 5] = nz;
    }
}

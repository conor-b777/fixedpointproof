import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class SimpleMinecraftClone {
    // Window
    private long window;
    private int width = 1280;
    private int height = 720;
    
    // Camera
    private float cameraX, cameraY, cameraZ;
    private float cameraRX, cameraRY;
    
    // World
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 64;
    private byte[][][] world = new byte[CHUNK_SIZE][WORLD_HEIGHT][CHUNK_SIZE];
    
    // Block types
    private static final byte AIR = 0;
    private static final byte GRASS = 1;
    private static final byte DIRT = 2;
    private static final byte STONE = 3;
    
    // Timing
    private float deltaTime;
    private long lastFrameTime;
    
    public static void main(String[] args) {
        new SimpleMinecraftClone().run();
    }
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Setup GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        // Create window
        window = glfwCreateWindow(width, height, "Minecraft Clone", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create window");
        }
        
        // Setup callbacks
        glfwSetKeyCallback(window, this::keyCallback);
        glfwSetCursorPosCallback(window, this::mouseCallback);
        glfwSetMouseButtonCallback(window, this::mouseButtonCallback);
        
        // Center window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        // Make OpenGL context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(window);
        
        // Initialize OpenGL
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.6f, 0.8f, 1.0f, 1.0f); // Sky color
        
        // Initialize camera
        cameraX = CHUNK_SIZE / 2f;
        cameraY = WORLD_HEIGHT / 2f;
        cameraZ = CHUNK_SIZE / 2f;
        
        // Generate simple world
        generateWorld();
        
        // Capture mouse
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }
    
    private void generateWorld() {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int height = (int)(Math.sin(x/4f) * 2 + (int)(Math.cos(z/4f) * 2) + WORLD_HEIGHT/2;
                
                for (int y = 0; y < WORLD_HEIGHT; y++) {
                    if (y > height) {
                        world[x][y][z] = AIR;
                    } else if (y == height) {
                        world[x][y][z] = GRASS;
                    } else if (y > height - 4) {
                        world[x][y][z] = DIRT;
                    } else {
                        world[x][y][z] = STONE;
                    }
                }
            }
        }
    }
    
    private void loop() {
        lastFrameTime = System.nanoTime();
        
        while (!glfwWindowShouldClose(window)) {
            // Calculate delta time
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = currentTime;
            
            // Poll events
            glfwPollEvents();
            
            // Update
            update();
            
            // Render
            render();
            
            // Swap buffers
            glfwSwapBuffers(window);
        }
    }
    
    private void update() {
        // Movement speed
        float speed = 5.0f * deltaTime;
        
        // Forward/backward
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            cameraX -= Math.sin(Math.toRadians(cameraRY)) * speed;
            cameraZ += Math.cos(Math.toRadians(cameraRY)) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            cameraX += Math.sin(Math.toRadians(cameraRY)) * speed;
            cameraZ -= Math.cos(Math.toRadians(cameraRY)) * speed;
        }
        
        // Left/right
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            cameraX -= Math.sin(Math.toRadians(cameraRY - 90)) * speed;
            cameraZ += Math.cos(Math.toRadians(cameraRY - 90)) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            cameraX -= Math.sin(Math.toRadians(cameraRY + 90)) * speed;
            cameraZ += Math.cos(Math.toRadians(cameraRY + 90)) * speed;
        }
        
        // Up/down (for testing)
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            cameraY += speed;
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            cameraY -= speed;
        }
        
        // Clamp camera position
        cameraX = Math.max(0, Math.min(CHUNK_SIZE - 1, cameraX));
        cameraY = Math.max(0, Math.min(WORLD_HEIGHT - 1, cameraY));
        cameraZ = Math.max(0, Math.min(CHUNK_SIZE - 1, cameraZ));
    }
    
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Set up projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float aspect = (float) width / height;
        gluPerspective(70.0f, aspect, 0.05f, 100.0f);
        
        // Set up modelview matrix
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        // Apply camera rotation and position
        glRotatef(cameraRX, 1, 0, 0);
        glRotatef(cameraRY, 0, 1, 0);
        glTranslatef(-cameraX, -cameraY, -cameraZ);
        
        // Render world
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (world[x][y][z] != AIR) {
                        renderBlock(x, y, z, world[x][y][z]);
                    }
                }
            }
        }
    }
    
    private void renderBlock(int x, int y, int z, byte type) {
        glPushMatrix();
        glTranslatef(x, y, z);
        
        // Set color based on block type
        switch (type) {
            case GRASS: glColor3f(0.2f, 0.8f, 0.3f); break;
            case DIRT: glColor3f(0.5f, 0.3f, 0.1f); break;
            case STONE: glColor3f(0.5f, 0.5f, 0.5f); break;
        }
        
        // Draw cube
        glBegin(GL_QUADS);
        // Front face
        glVertex3f(0, 0, 1);
        glVertex3f(1, 0, 1);
        glVertex3f(1, 1, 1);
        glVertex3f(0, 1, 1);
        
        // Back face
        glVertex3f(0, 0, 0);
        glVertex3f(0, 1, 0);
        glVertex3f(1, 1, 0);
        glVertex3f(1, 0, 0);
        
        // Top face
        glVertex3f(0, 1, 0);
        glVertex3f(0, 1, 1);
        glVertex3f(1, 1, 1);
        glVertex3f(1, 1, 0);
        
        // Bottom face
        glVertex3f(0, 0, 0);
        glVertex3f(1, 0, 0);
        glVertex3f(1, 0, 1);
        glVertex3f(0, 0, 1);
        
        // Left face
        glVertex3f(0, 0, 0);
        glVertex3f(0, 0, 1);
        glVertex3f(0, 1, 1);
        glVertex3f(0, 1, 0);
        
        // Right face
        glVertex3f(1, 0, 0);
        glVertex3f(1, 1, 0);
        glVertex3f(1, 1, 1);
        glVertex3f(1, 0, 1);
        glEnd();
        
        glPopMatrix();
    }
    
    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(window, true);
        }
    }
    
    private void mouseCallback(long window, double xpos, double ypos) {
        float sensitivity = 0.2f;
        
        if (glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            float dx = (float) (xpos - width/2.0);
            float dy = (float) (ypos - height/2.0);
            
            cameraRY += dx * sensitivity;
            cameraRX -= dy * sensitivity;
            cameraRX = Math.max(-89, Math.min(89, cameraRX));
            
            glfwSetCursorPos(window, width/2.0, height/2.0);
        }
    }
    
    private void mouseButtonCallback(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            // Break block
            breakBlock();
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
            // Place block
            placeBlock();
        }
    }
    
    private void breakBlock() {
        // Simple raycast to find block in front of player
        float maxDistance = 5.0f;
        float step = 0.1f;
        
        for (float t = 0; t < maxDistance; t += step) {
            int x = (int) (cameraX - Math.sin(Math.toRadians(cameraRY)) * t);
            int y = (int) (cameraY - Math.sin(Math.toRadians(cameraRX)) * t);
            int z = (int) (cameraZ + Math.cos(Math.toRadians(cameraRY)) * t);
            
            if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < WORLD_HEIGHT && z >= 0 && z < CHUNK_SIZE) {
                if (world[x][y][z] != AIR) {
                    world[x][y][z] = AIR;
                    return;
                }
            }
        }
    }
    
    private void placeBlock() {
        // Simple raycast to find empty space in front of player
        float maxDistance = 5.0f;
        float step = 0.1f;
        
        for (float t = 0; t < maxDistance; t += step) {
            int x = (int) (cameraX - Math.sin(Math.toRadians(cameraRY)) * t);
            int y = (int) (cameraY - Math.sin(Math.toRadians(cameraRX)) * t);
            int z = (int) (cameraZ + Math.cos(Math.toRadians(cameraRY)) * t);
            
            if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < WORLD_HEIGHT && z >= 0 && z < CHUNK_SIZE) {
                if (world[x][y][z] != AIR) {
                    // Place block in adjacent space
                    int px = (int) (cameraX - Math.sin(Math.toRadians(cameraRY)) * (t-step));
                    int py = (int) (cameraY - Math.sin(Math.toRadians(cameraRX)) * (t-step));
                    int pz = (int) (cameraZ + Math.cos(Math.toRadians(cameraRY)) * (t-step));
                    
                    if (px >= 0 && px < CHUNK_SIZE && py >= 0 && py < WORLD_HEIGHT && pz >= 0 && pz < CHUNK_SIZE) {
                        if (world[px][py][pz] == AIR) {
                            world[px][py][pz] = DIRT;
                        }
                    }
                    return;
                }
            }
        }
    }
    
    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
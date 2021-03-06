package lwjgui.glfw.input;

import org.lwjgl.glfw.GLFW;

import lwjgui.glfw.input.callbacks.MouseButtonCallback;
import lwjgui.glfw.input.callbacks.MouseEnterCallback;
import lwjgui.glfw.input.callbacks.MousePosCallback;
import lwjgui.glfw.input.callbacks.MouseScrollCallback;
import lwjgui.scene.Window;
import lwjgui.scene.WindowManager;

/**
 * 
 * Thread-safe object used to query Mouse input data from a {@link Window}.
 * 
 */
public class MouseHandler {

	private final MouseEnterCallback enterCallback;
	private final MousePosCallback posCallback;
	private final MouseButtonCallback buttonCallback;
	private final MouseScrollCallback scrollCallback;

	public MouseHandler(Window window) {
		this.enterCallback = new MouseEnterCallback();
		this.posCallback = new MousePosCallback();
		this.buttonCallback = new MouseButtonCallback();
		this.scrollCallback = new MouseScrollCallback();

		window.getCursorEnterCallback().addCallback(enterCallback);
		window.getCursorPosCallback().addCallback(posCallback);
		window.getMouseButtonCallback().addCallback(buttonCallback);
		window.getScrollCallback().addCallback(scrollCallback);
	}

	public void update() {
		this.posCallback.update();
	}

	public boolean isInside() {
		return this.enterCallback.isInside();
	}

	public float getX() {
		return (float) this.posCallback.getX();
	}

	public float getY() {
		return (float) this.posCallback.getY();
	}

	public int getXI() {
		return (int) this.posCallback.getX();
	}

	public int getYI() {
		return (int) this.posCallback.getY();
	}

	public float getDX() {
		return (float) this.posCallback.getDX();
	}

	public float getDY() {
		return (float) -this.posCallback.getDY();
	}

	public boolean isButtonPressed(int button) {
		return this.buttonCallback.isButtonPressed(button) && !this.buttonCallback.isButtonIgnored(button);
	}

	public void ignoreButtonUntilRelease(int button) {
		this.buttonCallback.setButtonIgnored(button);
	}

	public float getYWheel() {
		return (float) this.scrollCallback.getYWheel();
	}

	public float getXWheel() {
		return (float) this.scrollCallback.getXWheel();
	}

	public static void setGrabbed(long windowID, boolean grab) {
		WindowManager.runLater(() -> GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR,
				grab ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL));
	}
}

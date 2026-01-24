package com.astral.asttweaks.feature.notepad;

import com.astral.asttweaks.ASTTweaks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Notepad screen with multi-line text input using TextFieldWidgets.
 * Uses multiple TextFieldWidget instances for better IME compatibility.
 * Features: unified background, line numbers, scroll support, focus highlight.
 */
public class NotepadScreen extends Screen {
    private final NotepadStorage storage;
    private final List<TextFieldWidget> textFields = new ArrayList<>();

    // UI constants
    private static final int PADDING = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_SPACING = 10;
    private static final int LINE_HEIGHT = 16;
    private static final int MAX_TOTAL_LINES = 100;
    private static final int LINE_NUMBER_WIDTH = 28;
    private static final int EDITOR_PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 8;

    // Editor area
    private int editorX;
    private int editorY;
    private int editorWidth;
    private int editorHeight;
    private int textAreaX;
    private int textAreaWidth;

    // Scroll management
    private int scrollOffset = 0;
    private int visibleLines;

    // Scrollbar dragging
    private boolean isDraggingScrollbar = false;
    private double dragStartY;
    private int dragStartOffset;

    public NotepadScreen(NotepadStorage storage) {
        super(Text.translatable("screen." + ASTTweaks.MOD_ID + ".notepad.title"));
        this.storage = storage;
    }

    @Override
    protected void init() {
        super.init();

        textFields.clear();

        // Calculate editor area
        int totalWidth = Math.min(500, this.width - PADDING * 2);
        editorX = (this.width - totalWidth) / 2;
        editorY = PADDING + 24;
        editorWidth = totalWidth;
        editorHeight = this.height - editorY - PADDING - BUTTON_HEIGHT - 20;

        // Text area (excluding line numbers and scrollbar)
        textAreaX = editorX + LINE_NUMBER_WIDTH + EDITOR_PADDING;
        textAreaWidth = editorWidth - LINE_NUMBER_WIDTH - SCROLLBAR_WIDTH - EDITOR_PADDING * 2;

        // Calculate visible lines
        visibleLines = (editorHeight - EDITOR_PADDING * 2) / LINE_HEIGHT;

        // Parse existing content into lines
        String content = storage.getContent();
        String[] lines = content.split("\n", -1);

        // Create text fields for all lines
        for (int i = 0; i < MAX_TOTAL_LINES; i++) {
            TextFieldWidget textField = new TextFieldWidget(
                    this.textRenderer,
                    textAreaX,
                    editorY + EDITOR_PADDING + i * LINE_HEIGHT,
                    textAreaWidth,
                    LINE_HEIGHT - 2,
                    Text.literal("")
            );
            textField.setMaxLength(500);
            textField.setDrawsBackground(false);  // Disable individual backgrounds

            // Set initial text if available
            if (i < lines.length) {
                textField.setText(lines[i]);
            }

            final int lineIndex = i;
            textField.setChangedListener(text -> onLineChanged(lineIndex, text));

            this.addDrawableChild(textField);
            textFields.add(textField);
        }

        // Update visibility based on scroll
        updateVisibleFields();

        // Focus first text field
        if (!textFields.isEmpty()) {
            setInitialFocus(textFields.get(0));
        }

        // Button positions
        int buttonY = this.height - PADDING - BUTTON_HEIGHT;
        int totalButtonWidth = BUTTON_WIDTH * 2 + BUTTON_SPACING;
        int buttonStartX = (this.width - totalButtonWidth) / 2;

        // Save button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".notepad.save"),
                button -> saveAndClose()
        ).dimensions(buttonStartX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen." + ASTTweaks.MOD_ID + ".notepad.close"),
                button -> close()
        ).dimensions(buttonStartX + BUTTON_WIDTH + BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void updateVisibleFields() {
        for (int i = 0; i < textFields.size(); i++) {
            TextFieldWidget field = textFields.get(i);
            boolean isVisible = i >= scrollOffset && i < scrollOffset + visibleLines;

            if (isVisible) {
                int visibleIndex = i - scrollOffset;
                field.setY(editorY + EDITOR_PADDING + visibleIndex * LINE_HEIGHT);
                field.visible = true;
                field.active = true;
            } else {
                field.visible = false;
                field.active = false;
            }
        }
    }

    private int getMaxScroll() {
        return Math.max(0, MAX_TOTAL_LINES - visibleLines);
    }

    private void onLineChanged(int lineIndex, String text) {
        // Handle Enter key detection by checking for newline character
        if (text.contains("\n")) {
            // Remove newline and move to next line
            String cleanText = text.replace("\n", "");
            textFields.get(lineIndex).setText(cleanText);

            // Move focus to next line
            if (lineIndex + 1 < textFields.size()) {
                moveToLine(lineIndex + 1, true);
            }
        }
    }

    private void moveToLine(int lineIndex, boolean toStart) {
        // Ensure line is visible
        if (lineIndex < scrollOffset) {
            scrollOffset = lineIndex;
            updateVisibleFields();
        } else if (lineIndex >= scrollOffset + visibleLines) {
            scrollOffset = lineIndex - visibleLines + 1;
            updateVisibleFields();
        }

        TextFieldWidget field = textFields.get(lineIndex);
        if (toStart) {
            field.setCursorToStart();
        }
        setFocused(field);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key to move to next line
        if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            TextFieldWidget focused = getFocusedTextField();
            if (focused != null) {
                int index = textFields.indexOf(focused);
                if (index >= 0 && index + 1 < textFields.size()) {
                    moveToLine(index + 1, true);
                    return true;
                }
            }
        }

        // Handle Up arrow to move to previous line
        if (keyCode == 265) { // Up arrow
            TextFieldWidget focused = getFocusedTextField();
            if (focused != null) {
                int index = textFields.indexOf(focused);
                if (index > 0) {
                    moveToLine(index - 1, false);
                    return true;
                }
            }
        }

        // Handle Down arrow to move to next line
        if (keyCode == 264) { // Down arrow
            TextFieldWidget focused = getFocusedTextField();
            if (focused != null) {
                int index = textFields.indexOf(focused);
                if (index >= 0 && index + 1 < textFields.size()) {
                    moveToLine(index + 1, false);
                    return true;
                }
            }
        }

        // Page Up
        if (keyCode == 266) {
            scrollOffset = Math.max(0, scrollOffset - visibleLines);
            updateVisibleFields();
            return true;
        }

        // Page Down
        if (keyCode == 267) {
            scrollOffset = Math.min(getMaxScroll(), scrollOffset + visibleLines);
            updateVisibleFields();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseInEditor(mouseX, mouseY)) {
            int scrollAmount = (int) (-amount * 3);
            scrollOffset = MathHelper.clamp(scrollOffset + scrollAmount, 0, getMaxScroll());
            updateVisibleFields();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on scrollbar
        if (button == 0 && isMouseOnScrollbar(mouseX, mouseY)) {
            isDraggingScrollbar = true;
            dragStartY = mouseY;
            dragStartOffset = scrollOffset;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            int trackHeight = editorHeight - EDITOR_PADDING * 2 - getScrollbarHeight();
            if (trackHeight > 0) {
                double dragDelta = mouseY - dragStartY;
                int scrollDelta = (int) (dragDelta * getMaxScroll() / trackHeight);
                scrollOffset = MathHelper.clamp(dragStartOffset + scrollDelta, 0, getMaxScroll());
                updateVisibleFields();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean isMouseInEditor(double mouseX, double mouseY) {
        return mouseX >= editorX && mouseX < editorX + editorWidth
                && mouseY >= editorY && mouseY < editorY + editorHeight;
    }

    private boolean isMouseOnScrollbar(double mouseX, double mouseY) {
        int scrollbarX = editorX + editorWidth - SCROLLBAR_WIDTH - 2;
        int scrollbarY = editorY + EDITOR_PADDING + getScrollbarY();
        int scrollbarHeight = getScrollbarHeight();

        return mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight;
    }

    private int getScrollbarY() {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return 0;

        int trackHeight = editorHeight - EDITOR_PADDING * 2 - getScrollbarHeight();
        return (int) ((float) scrollOffset / maxScroll * trackHeight);
    }

    private int getScrollbarHeight() {
        int trackHeight = editorHeight - EDITOR_PADDING * 2;
        float ratio = (float) visibleLines / MAX_TOTAL_LINES;
        return Math.max(20, (int) (trackHeight * ratio));
    }

    private TextFieldWidget getFocusedTextField() {
        for (TextFieldWidget field : textFields) {
            if (field.isFocused()) {
                return field;
            }
        }
        return null;
    }

    private int getFocusedLineIndex() {
        for (int i = 0; i < textFields.size(); i++) {
            if (textFields.get(i).isFocused()) {
                return i;
            }
        }
        return -1;
    }

    private void saveAndClose() {
        // Collect all lines into content
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < textFields.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(textFields.get(i).getText());
        }

        // Trim trailing empty lines but preserve intentional content
        String content = sb.toString().replaceAll("\\n+$", "");

        storage.setContent(content);
        storage.save();
        close();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // Draw title
        drawCenteredTextWithShadow(
                matrices,
                this.textRenderer,
                this.title,
                this.width / 2,
                PADDING / 2 + 4,
                0xFFFFFF
        );

        // Draw editor background
        renderEditorBackground(matrices);

        // Draw line numbers
        renderLineNumbers(matrices);

        // Draw scrollbar
        renderScrollbar(matrices);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderEditorBackground(MatrixStack matrices) {
        // Main background (light cream/notepad color)
        fill(matrices, editorX, editorY,
                editorX + editorWidth, editorY + editorHeight,
                0xFFFFFFF0);

        // Line number area (slightly darker)
        fill(matrices, editorX, editorY,
                editorX + LINE_NUMBER_WIDTH, editorY + editorHeight,
                0xFFE8E8E0);

        // Separator line between line numbers and text
        fill(matrices, editorX + LINE_NUMBER_WIDTH, editorY,
                editorX + LINE_NUMBER_WIDTH + 1, editorY + editorHeight,
                0xFFD0D0C0);

        // Focused line highlight
        int focusedLine = getFocusedLineIndex();
        if (focusedLine >= scrollOffset && focusedLine < scrollOffset + visibleLines) {
            int highlightY = editorY + EDITOR_PADDING + (focusedLine - scrollOffset) * LINE_HEIGHT;
            fill(matrices, editorX + LINE_NUMBER_WIDTH + 1, highlightY,
                    editorX + editorWidth - SCROLLBAR_WIDTH - 2, highlightY + LINE_HEIGHT,
                    0x30000080);  // Semi-transparent blue
        }

        // Border
        drawHorizontalLine(matrices, editorX - 1, editorX + editorWidth, editorY - 1, 0xFF808080);
        drawHorizontalLine(matrices, editorX - 1, editorX + editorWidth, editorY + editorHeight, 0xFF808080);
        drawVerticalLine(matrices, editorX - 1, editorY - 1, editorY + editorHeight, 0xFF808080);
        drawVerticalLine(matrices, editorX + editorWidth, editorY - 1, editorY + editorHeight, 0xFF808080);
    }

    private void renderLineNumbers(MatrixStack matrices) {
        for (int i = 0; i < visibleLines; i++) {
            int lineNumber = scrollOffset + i + 1;
            if (lineNumber > MAX_TOTAL_LINES) break;

            String lineNumStr = String.valueOf(lineNumber);
            int lineNumWidth = this.textRenderer.getWidth(lineNumStr);
            int x = editorX + LINE_NUMBER_WIDTH - lineNumWidth - 4;
            int y = editorY + EDITOR_PADDING + i * LINE_HEIGHT + 3;

            // Highlight current line number
            int focusedLine = getFocusedLineIndex();
            int color = (focusedLine == scrollOffset + i) ? 0xFF404080 : 0xFF808080;

            this.textRenderer.draw(matrices, lineNumStr, x, y, color);
        }
    }

    private void renderScrollbar(MatrixStack matrices) {
        int scrollbarX = editorX + editorWidth - SCROLLBAR_WIDTH - 2;
        int trackY = editorY + EDITOR_PADDING;
        int trackHeight = editorHeight - EDITOR_PADDING * 2;

        // Scrollbar track
        fill(matrices, scrollbarX, trackY,
                scrollbarX + SCROLLBAR_WIDTH, trackY + trackHeight,
                0xFFD0D0D0);

        // Scrollbar thumb
        int thumbY = trackY + getScrollbarY();
        int thumbHeight = getScrollbarHeight();
        int thumbColor = isDraggingScrollbar ? 0xFF606060 : 0xFF909090;

        fill(matrices, scrollbarX + 1, thumbY,
                scrollbarX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                thumbColor);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

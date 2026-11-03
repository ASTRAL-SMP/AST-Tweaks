package com.astral.asttweaks.feature.notepad;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.util.McClient;
import com.astral.asttweaks.util.gui.CompatScreen;
import com.astral.asttweaks.util.gui.Gfx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Notepad screen with multi-line text input using TextFieldWidgets.
 * Uses multiple TextFieldWidget instances for better IME compatibility.
 * Features: unified background, line numbers, scroll support, focus highlight.
 */
public class NotepadScreen extends CompatScreen {
    private final NotepadStorage storage;
    private final List<EditBox> textFields = new ArrayList<>();

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
        super(Component.translatable("screen." + ASTTweaks.MOD_ID + ".notepad.title"));
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
            EditBox textField = new EditBox(
                    this.font,
                    textAreaX,
                    editorY + EDITOR_PADDING + i * LINE_HEIGHT,
                    textAreaWidth,
                    LINE_HEIGHT - 2,
                    Component.literal("")
            );
            textField.setMaxLength(500);
            textField.setBordered(false);  // Disable individual backgrounds

            // Set initial text if available
            if (i < lines.length) {
                textField.setValue(lines[i]);
            }

            final int lineIndex = i;
            textField.setResponder(text -> onLineChanged(lineIndex, text));

            this.addRenderableWidget(textField);
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
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".notepad.save"),
                button -> saveAndClose()
        ).bounds(buttonStartX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        // Close button
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen." + ASTTweaks.MOD_ID + ".notepad.close"),
                button -> onClose()
        ).bounds(buttonStartX + BUTTON_WIDTH + BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void updateVisibleFields() {
        for (int i = 0; i < textFields.size(); i++) {
            EditBox field = textFields.get(i);
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
            textFields.get(lineIndex).setValue(cleanText);

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

        EditBox field = textFields.get(lineIndex);
        if (toStart) {
            //? if >=1.20.3 {
            field.moveCursorToStart(false);
            //?} else {
            /*field.moveCursorToStart();
            *///?}
        }
        setFocused(field);
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scancode, int modifiers) {
        // Handle Enter key to move to next line
        if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            EditBox focused = getFocusedTextField();
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
            EditBox focused = getFocusedTextField();
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
            EditBox focused = getFocusedTextField();
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

        return superKeyPressed(keyCode, scancode, modifiers);
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseInEditor(mouseX, mouseY)) {
            int scrollAmount = (int) (-amount * 3);
            scrollOffset = Mth.clamp(scrollOffset + scrollAmount, 0, getMaxScroll());
            updateVisibleFields();
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on scrollbar
        if (button == 0 && isMouseOnScrollbar(mouseX, mouseY)) {
            isDraggingScrollbar = true;
            dragStartY = mouseY;
            dragStartOffset = scrollOffset;
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            int trackHeight = editorHeight - EDITOR_PADDING * 2 - getScrollbarHeight();
            if (trackHeight > 0) {
                double dragDelta = mouseY - dragStartY;
                int scrollDelta = (int) (dragDelta * getMaxScroll() / trackHeight);
                scrollOffset = Mth.clamp(dragStartOffset + scrollDelta, 0, getMaxScroll());
                updateVisibleFields();
            }
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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

    private EditBox getFocusedTextField() {
        for (EditBox field : textFields) {
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
            sb.append(textFields.get(i).getValue());
        }

        // Trim trailing empty lines but preserve intentional content
        String content = sb.toString().replaceAll("\\n+$", "");

        storage.setContent(content);
        storage.save();
        onClose();
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        // Draw title
        gfx.centeredText(
                this.font,
                this.title,
                this.width / 2,
                PADDING / 2 + 4,
                0xFFFFFFFF
        );

        // Draw editor background
        renderEditorBackground(gfx);

        // Draw line numbers
        renderLineNumbers(gfx);

        // Draw scrollbar
        renderScrollbar(gfx);

        renderWidgets(gfx, mouseX, mouseY, delta);
    }

    private void renderEditorBackground(Gfx gfx) {
        // Main background (light cream/notepad color)
        gfx.fill(editorX, editorY,
                editorX + editorWidth, editorY + editorHeight,
                0xFFFFFFF0);

        // Line number area (slightly darker)
        gfx.fill(editorX, editorY,
                editorX + LINE_NUMBER_WIDTH, editorY + editorHeight,
                0xFFE8E8E0);

        // Separator line between line numbers and text
        gfx.fill(editorX + LINE_NUMBER_WIDTH, editorY,
                editorX + LINE_NUMBER_WIDTH + 1, editorY + editorHeight,
                0xFFD0D0C0);

        // Focused line highlight
        int focusedLine = getFocusedLineIndex();
        if (focusedLine >= scrollOffset && focusedLine < scrollOffset + visibleLines) {
            int highlightY = editorY + EDITOR_PADDING + (focusedLine - scrollOffset) * LINE_HEIGHT;
            gfx.fill(editorX + LINE_NUMBER_WIDTH + 1, highlightY,
                    editorX + editorWidth - SCROLLBAR_WIDTH - 2, highlightY + LINE_HEIGHT,
                    0x30000080);  // Semi-transparent blue
        }

        // Border
        gfx.hLine(editorX - 1, editorX + editorWidth, editorY - 1, 0xFF808080);
        gfx.hLine(editorX - 1, editorX + editorWidth, editorY + editorHeight, 0xFF808080);
        gfx.vLine(editorX - 1, editorY - 1, editorY + editorHeight, 0xFF808080);
        gfx.vLine(editorX + editorWidth, editorY - 1, editorY + editorHeight, 0xFF808080);
    }

    private void renderLineNumbers(Gfx gfx) {
        for (int i = 0; i < visibleLines; i++) {
            int lineNumber = scrollOffset + i + 1;
            if (lineNumber > MAX_TOTAL_LINES) break;

            String lineNumStr = String.valueOf(lineNumber);
            int lineNumWidth = this.font.width(lineNumStr);
            int x = editorX + LINE_NUMBER_WIDTH - lineNumWidth - 4;
            int y = editorY + EDITOR_PADDING + i * LINE_HEIGHT + 3;

            // Highlight current line number
            int focusedLine = getFocusedLineIndex();
            int color = (focusedLine == scrollOffset + i) ? 0xFF404080 : 0xFF808080;

            gfx.text(this.font, lineNumStr, x, y, color, false);
        }
    }

    private void renderScrollbar(Gfx gfx) {
        int scrollbarX = editorX + editorWidth - SCROLLBAR_WIDTH - 2;
        int trackY = editorY + EDITOR_PADDING;
        int trackHeight = editorHeight - EDITOR_PADDING * 2;

        // Scrollbar track
        gfx.fill(scrollbarX, trackY,
                scrollbarX + SCROLLBAR_WIDTH, trackY + trackHeight,
                0xFFD0D0D0);

        // Scrollbar thumb
        int thumbY = trackY + getScrollbarY();
        int thumbHeight = getScrollbarHeight();
        int thumbColor = isDraggingScrollbar ? 0xFF606060 : 0xFF909090;

        gfx.fill(scrollbarX + 1, thumbY,
                scrollbarX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                thumbColor);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            McClient.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

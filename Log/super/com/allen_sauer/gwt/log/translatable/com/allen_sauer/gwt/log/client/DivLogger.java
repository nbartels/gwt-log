/*
 * Copyright 2009 Fred Sauer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.allen_sauer.gwt.log.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.allen_sauer.gwt.log.client.impl.LogClientBundle;
import com.allen_sauer.gwt.log.client.util.DOMUtil;
import com.allen_sauer.gwt.log.client.util.LogUtil;
import com.allen_sauer.gwt.log.clientserverdemo.client.LogClientServerDemo;

/**
 * Logger which outputs to a draggable floating <code>DIV</code>.
 */
public class DivLogger extends AbstractLogger {
  // CHECKSTYLE_JAVADOC_OFF

  private class MouseDragHandler implements MouseMoveHandler, MouseUpHandler, MouseDownHandler {
    private boolean dragging = false;
    private final Label dragHandle;
    private int dragStartX;
    private int dragStartY;

    public MouseDragHandler(Label dragHandle) {
      this.dragHandle = dragHandle;
      dragHandle.addMouseDownHandler(this);
      dragHandle.addMouseUpHandler(this);
      dragHandle.addMouseMoveHandler(this);
    }

    public void onMouseDown(MouseDownEvent event) {
      dragging = true;
      dragStartX = event.getRelativeX(dragHandle.getElement());
      dragStartY = event.getRelativeY(dragHandle.getElement());
      DOM.setCapture(dragHandle.getElement());
    }

    public void onMouseMove(MouseMoveEvent event) {
      if (dragging) {
        int absX = event.getRelativeX(dragHandle.getElement()) + logDockPanel.getAbsoluteLeft();
        int absY = event.getRelativeY(dragHandle.getElement()) + logDockPanel.getAbsoluteTop();
        RootPanel.get().setWidgetPosition(logDockPanel, absX - dragStartX, absY - dragStartY);
      }
    }

    public void onMouseUp(MouseUpEvent event) {
      dragging = false;
      DOM.releaseCapture(dragHandle.getElement());
    }
  }

  private class MouseResizeHandler implements MouseMoveHandler, MouseUpHandler, MouseDownHandler {
    private boolean dragging = false;
    private int dragStartX;
    private int dragStartY;
    private final Widget resizePanel;

    public MouseResizeHandler(Widget resizePanel) {
      this.resizePanel = resizePanel;
      HasAllMouseHandlers hamh = (HasAllMouseHandlers) resizePanel;
      hamh.addMouseMoveHandler(this);
      hamh.addMouseDownHandler(this);
      hamh.addMouseUpHandler(this);
    }

    public void onMouseDown(MouseDownEvent event) {
      dragging = true;
      DOM.setCapture(resizePanel.getElement());
      dragStartX = event.getX();
      dragStartY = event.getY();
      DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
    }

    public void onMouseMove(MouseMoveEvent event) {
      if (dragging) {
        scrollPanel.incrementPixelSize(event.getX() - dragStartX, event.getY() - dragStartY);
        scrollPanel.setScrollPosition(Integer.MAX_VALUE);
      }
    }

    public void onMouseUp(MouseUpEvent event) {
      dragging = false;
      DOM.releaseCapture(resizePanel.getElement());
    }
  }

  private static class ScrollPanelImpl extends ScrollPanel {
    private int minScrollPanelHeight = -1;
    private int minScrollPanelWidth = -1;
    private int scrollPanelHeight;
    private int scrollPanelWidth;

    public void checkMinSize() {
      if (minScrollPanelWidth == -1) {
        // need try-catch for certain initialization cases such as an early
        // JavaScript alert()
        try {
          minScrollPanelWidth = getOffsetWidth();
          minScrollPanelHeight = getOffsetHeight();
        } catch (Throwable ignore) {
        }
      }
    }

    public void incrementPixelSize(int width, int height) {
      setPixelSize(scrollPanelWidth + width, scrollPanelHeight + height);
    }

    @Override
    public void setPixelSize(int width, int height) {
      super.setPixelSize(scrollPanelWidth = Math.max(width, minScrollPanelWidth),
          scrollPanelHeight = Math.max(height, minScrollPanelHeight));
    }
  }

  private static final int[] levels = {
      Log.LOG_LEVEL_TRACE, Log.LOG_LEVEL_DEBUG, Log.LOG_LEVEL_INFO, Log.LOG_LEVEL_WARN,
      Log.LOG_LEVEL_ERROR, Log.LOG_LEVEL_FATAL, Log.LOG_LEVEL_OFF,};
  private static final String STACKTRACE_ELEMENT_PREFIX = "&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;";

  private static final int UPDATE_INTERVAL_MILLIS = 500;
  private boolean dirty = false;
  private Button[] levelButtons;
  private final DockPanel logDockPanel = new DockPanel() {
    private int lastDocumentClientHeight = -1;
    private int lastDocumentClientWidth = -1;

    private HandlerRegistration resizeRegistration;
    private final ResizeHandler windowResizeListener = new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        int width = event.getWidth();
        int height = event.getHeight();
        resize(width, height);
      }
    };

    @Override
    public void setVisible(boolean visible) {
      super.setVisible(visible);
      if (visible) {
        scrollPanel.checkMinSize();
        resize(Window.getClientWidth(), Window.getClientHeight());
      }
    }

    @Override
    protected void onLoad() {
      super.onLoad();
      resizeRegistration = Window.addResizeHandler(windowResizeListener);
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      resizeRegistration.removeHandler();
    }

    private void resize(int width, int height) {
      // Workaround for issue 1934
      // IE fires Window onresize events when the size of the body changes
      if (width != lastDocumentClientWidth || height != lastDocumentClientHeight) {
        lastDocumentClientWidth = width;
        lastDocumentClientHeight = height;

        scrollPanel.setPixelSize(Math.max(300, (int) (Window.getClientWidth() * .8)), Math.max(100,
            (int) (Window.getClientHeight() * .3)));
      }
    }
  };
  private String logText = "";

  private final HTML logTextArea = new HTML();

  private final ScrollPanelImpl scrollPanel = new ScrollPanelImpl();
  private final Timer timer;

  /**
   * Default constructor.
   */
  public DivLogger() {
    logDockPanel.addStyleName(LogClientBundle.INSTANCE.css().logPanel());
    logTextArea.addStyleName(LogClientBundle.INSTANCE.css().logTextArea());
    scrollPanel.addStyleName(LogClientBundle.INSTANCE.css().logScrollPanel());

    // scrollPanel.setAlwaysShowScrollBars(true);

    final FocusPanel headerPanel = makeHeader();

    Widget resizePanel = new Image(GWT.getModuleBaseURL() + "gwt-log-triangle-10x10.png");
    resizePanel.addStyleName(LogClientBundle.INSTANCE.css().logResizeSe());
    new MouseResizeHandler(resizePanel);

    logDockPanel.add(headerPanel, DockPanel.NORTH);
    logDockPanel.add(scrollPanel, DockPanel.CENTER);
    logDockPanel.add(resizePanel, DockPanel.SOUTH);
    DOM.setStyleAttribute(DOM.getParent(resizePanel.getElement()), "lineHeight", "1px");
    logDockPanel.setCellHorizontalAlignment(resizePanel, HasHorizontalAlignment.ALIGN_RIGHT);

    scrollPanel.setWidget(logTextArea);

    logDockPanel.setVisible(false);
    RootPanel.get().add(logDockPanel, 0, 0);

    timer = new Timer() {
      @Override
      public void run() {
        dirty = false;
        logTextArea.setHTML(logTextArea.getHTML() + logText);
        logText = "";
        DeferredCommand.addCommand(new Command() {
          public void execute() {
            scrollPanel.setScrollPosition(0x8888888);
          }
        });
      }
    };
  }

  @Override
  public final void clear() {
    logTextArea.setHTML("");
  }

  public final Widget getWidget() {
    return logDockPanel;
  }

  public final boolean isSupported() {
    return true;
  }

  public final boolean isVisible() {
    return logDockPanel.isAttached() && logDockPanel.isVisible();
  }

  public final void moveTo(int x, int y) {
    RootPanel.get().add(logDockPanel, x, y);
  }

  @Override
  public void setCurrentLogLevel(int level) {
    super.setCurrentLogLevel(level);
    for (int i = 0; i < levels.length; i++) {
      if (levels[i] < Log.getLowestLogLevel()) {
        levelButtons[i].setEnabled(false);
      } else {
        String levelText = LogUtil.levelToString(levels[i]);
        boolean current = level == levels[i];
        levelButtons[i].setTitle(current ? "Current (runtime) log level is already '" + levelText
            + "'" : "Set current (runtime) log level to '" + levelText + "'");
        boolean active = level <= levels[i];
        DOM.setStyleAttribute(levelButtons[i].getElement(), "color", active ? getColor(levels[i])
            : "#ccc");
      }
    }
  }

  public final void setPixelSize(int width, int height) {
    logTextArea.setPixelSize(width, height);
  }

  public final void setSize(String width, String height) {
    logTextArea.setSize(width, height);
  }

  @Override
  final void log(int logLevel, String message) {
    assert false;
    // Method never called since {@link #log(int, String, Throwable)} is
    // overridden
  }

  @Override
  final void log(int logLevel, String message, Throwable throwable) {
    logDockPanel.setVisible(true);
    String text = message.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    String title = makeTitle(message, throwable);
    if (throwable != null) {
      while (throwable != null) {
        text += throwable.getClass().getName() + ":<br><b>" + throwable.getMessage() + "</b>";
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements.length > 0) {
          text += "<div class='log-stacktrace'>";
          for (StackTraceElement element : stackTraceElements) {
            text += STACKTRACE_ELEMENT_PREFIX + element + "<br>";
          }
          text += "</div>";
        }
        throwable = throwable.getCause();
        if (throwable != null) {
          text += "Caused by: ";
        }
      }
    }
    text = text.replaceAll("\r\n|\r|\n", "<BR>");
    addLogText("<div class='" + LogClientBundle.INSTANCE.css().logMessage()
        + "' onmouseover='className+=\" log-message-hover\"' "
        + "onmouseout='className=className.replace(/ log-message-hover/g,\"\")' style='color: "
        + getColor(logLevel) + "' title='" + title + "'>" + text + "</div>");
  }

  private void addLogText(String logTest) {
    logText += logTest;
    if (!dirty) {
      dirty = true;
      timer.schedule(UPDATE_INTERVAL_MILLIS);
    }
  }

  private String getColor(int logLevel) {
    if (logLevel == Log.LOG_LEVEL_OFF) {
      return "#000"; // black
    }
    if (logLevel >= Log.LOG_LEVEL_FATAL) {
      return "#F00"; // bright red
    }
    if (logLevel >= Log.LOG_LEVEL_ERROR) {
      return "#C11B17"; // dark red
    }
    if (logLevel >= Log.LOG_LEVEL_WARN) {
      return "#E56717"; // dark orange
    }
    if (logLevel >= Log.LOG_LEVEL_INFO) {
      return "#2B60DE"; // blue
    }
    if (logLevel >= Log.LOG_LEVEL_DEBUG) {
      return "#20b000"; // green
    }
    return "#F0F"; // purple
  }

  /**
   * @deprecated
   */
  @Deprecated
  private FocusPanel makeHeader() {
    FocusPanel header;
    header = new FocusPanel();
    HorizontalPanel masterPanel = new HorizontalPanel();
    masterPanel.setWidth("100%");
    header.add(masterPanel);

    final Label titleLabel = new Label("gwt-log", false);
    titleLabel.setStylePrimaryName(LogClientBundle.INSTANCE.css().logTitle());

    HorizontalPanel buttonPanel = new HorizontalPanel();
    levelButtons = new Button[levels.length];
    for (int i = 0; i < levels.length; i++) {
      final int level = levels[i];
      levelButtons[i] = new Button(LogUtil.levelToString(level));
      buttonPanel.add(levelButtons[i]);
      levelButtons[i].addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          ((Button) event.getSource()).setFocus(false);
          Log.setCurrentLogLevel(level);
        }
      });
    }

    Button clearButton = new Button("Clear");
    clearButton.addStyleName(LogClientBundle.INSTANCE.css().logClearButton());
    DOM.setStyleAttribute(clearButton.getElement(), "color", "#00c");
    clearButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        ((Button) event.getSource()).setFocus(false);
        Log.clear();
      }
    });
    buttonPanel.add(clearButton);

    Button aboutButton = new Button("About");
    aboutButton.addStyleName(LogClientBundle.INSTANCE.css().logClearAbout());
    aboutButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        ((Button) event.getSource()).setFocus(false);

        Log.diagnostic("\n" //
            + "gwt-log-" + Log.getVersion() //
            + " - Runtime logging for your Google Web Toolkit projects\n" + //
            "Copyright 2007-2008 Fred Sauer\n" + //
            "The original software is available from:\n" + //
            "\u00a0\u00a0\u00a0\u00a0http://allen-sauer.com/gwt/\n", null);
      }
    });

    masterPanel.add(titleLabel);
    masterPanel.add(buttonPanel);
    masterPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    masterPanel.add(aboutButton);

    masterPanel.setCellHeight(titleLabel, "100%");
    masterPanel.setCellWidth(titleLabel, "50%");
    masterPanel.setCellWidth(aboutButton, "50%");

    new MouseDragHandler(titleLabel);

    return header;
  }

  private String makeTitle(String message, Throwable throwable) {
    if (throwable != null) {
      if (throwable.getMessage() == null) {
        message = throwable.getClass().getName();
      } else {
        message = throwable.getMessage().replaceAll(
            throwable.getClass().getName().replaceAll("^(.+\\.).+$", "$1"), "");
      }
    }
    return DOMUtil.adjustTitleLineBreaks(message).replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
        "'", "\"");
  }
}
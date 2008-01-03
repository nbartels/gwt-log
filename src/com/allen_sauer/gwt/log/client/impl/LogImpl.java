/*
 * Copyright 2007 Fred Sauer
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
package com.allen_sauer.gwt.log.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;

import com.allen_sauer.gwt.log.client.ConsoleLogger;
import com.allen_sauer.gwt.log.client.DivLogger;
import com.allen_sauer.gwt.log.client.FirebugLogger;
import com.allen_sauer.gwt.log.client.GWTLogger;
import com.allen_sauer.gwt.log.client.Logger;
import com.allen_sauer.gwt.log.client.LoggerSystem;

public abstract class LogImpl {
  static final String LOG_LEVEL_TEXT_DEBUG = "DEBUG";
  static final String LOG_LEVEL_TEXT_ERROR = "ERROR";
  static final String LOG_LEVEL_TEXT_FATAL = "FATAL";
  static final String LOG_LEVEL_TEXT_INFO = "INFO";
  static final String LOG_LEVEL_TEXT_OFF = "OFF";
  static final String LOG_LEVEL_TEXT_WARN = "WARN";

  public abstract void addLogger(Logger logger);

  public abstract void clear();

  public abstract void debug(String message, JavaScriptObject e);

  public abstract void debug(String message, Throwable e);

  public abstract void error(String message, JavaScriptObject e);

  public abstract void error(String message, Throwable e);

  public abstract void fatal(String message, JavaScriptObject e);

  public abstract void fatal(String message, Throwable e);

  public abstract int getCurrentLogLevel();

  public abstract String getCurrentLogLevelString();

  public abstract ConsoleLogger getLoggerConsole();

  public abstract DivLogger getLoggerDiv();

  public abstract FirebugLogger getLoggerFirebug();

  public abstract GWTLogger getLoggerGWT();

  public abstract LoggerSystem getLoggerSystem();

  public abstract int getLowestLogLevel();

  public abstract String getLowestLogLevelString();

  public abstract void info(String message, JavaScriptObject e);

  public abstract void info(String message, Throwable e);

  public abstract boolean isDebugEnabled();

  public abstract boolean isErrorEnabled();

  public abstract boolean isFatalEnabled();

  public abstract boolean isInfoEnabled();

  public abstract boolean isLoggingEnabled();

  public abstract boolean isWarnEnabled();

  public abstract boolean removeLogger(Logger logger);

  public int setCurrentLogLevel(int level) {
    if (level < getLowestLogLevel()) {
      Window.alert("Unable to lower runtime log level to " + level
          + " due to compile time minimum of " + getLowestLogLevel());
      level = getLowestLogLevel();
    }
    return level;
  }

  public abstract void setUncaughtExceptionHandler();

  public abstract void warn(String message, JavaScriptObject e);

  public abstract void warn(String message, Throwable e);
}

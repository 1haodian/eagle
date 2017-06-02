package org.apache.eagle.alert.engine.coder;

import java.io.Serializable;

public class MyType implements Serializable{
  private String type;
  private long value;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public long getValue() {
    return value;
  }

  public void setValue(long value) {
    this.value = value;
  }
}

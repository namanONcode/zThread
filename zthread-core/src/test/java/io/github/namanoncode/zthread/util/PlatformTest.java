package io.github.namanoncode.zthread.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class PlatformTest {

  @Test
  @EnabledOnOs(OS.LINUX)
  void detectsLinux() {
    assertThat(Platform.isLinux()).isTrue();
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  void ensureLinuxDoesNotThrow() {
    Platform.ensureLinux();
  }

  @Test
  void architectureDetection() {
    assertThat(Platform.OS_ARCH).isNotBlank();
  }
}

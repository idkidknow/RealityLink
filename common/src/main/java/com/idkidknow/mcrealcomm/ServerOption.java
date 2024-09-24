package com.idkidknow.mcrealcomm;

import java.nio.file.Path;

public record ServerOption(int port, String localeCode, String resourcePacksDir) {
}

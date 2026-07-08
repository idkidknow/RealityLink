{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };
  outputs =
    { nixpkgs, ... }:
    let
      pkgs = nixpkgs.legacyPackages.x86_64-linux;
      lib = pkgs.lib;
      runtimeLibs = with pkgs; [
        (lib.getLib stdenv.cc.cc)
        glfw3-minecraft
        openal
        alsa-lib
        libjack2
        libpulseaudio
        pipewire
        libGL
        libx11
        libxcursor
        libxext
        libxrandr
        libxxf86vm
        wayland
        udev
        vulkan-loader
        flite
      ];
    in
    {
      devShells.x86_64-linux.default = pkgs.mkShell rec {
        buildInputs = with pkgs; [
          jdk25_headless
          metals
        ];

        shellHook = ''
          export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:${pkgs.lib.makeLibraryPath runtimeLibs}"
          export MILL_NO_SEPARATE_BSP_OUTPUT_DIR=1
        '';
      };
    };
}

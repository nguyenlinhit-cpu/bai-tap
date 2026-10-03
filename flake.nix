{
  description = "Bài Việt – Tổng Hợp Game Bài – Android Kotlin dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # ---------- Android SDK ----------
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          # Build-tools & platform matching project requirements
          buildToolsVersions = [ "35.0.0" "34.0.0" ];
          platformVersions   = [ "35" "34" ];
          # compileSdk / targetSdk latest (35); minSdk 26

          includeNDK          = false;
          includeSources      = false;
          includeSystemImages = false;

          extraLicenses = [
            "android-googletv-license"
            "android-sdk-arm-dbt-license"
            "android-sdk-license"
            "android-sdk-preview-license"
            "google-gdk-license"
            "intel-android-extra-license"
            "mips-android-sysimage-license"
          ];
        };

        androidSdk = androidComposition.androidsdk;

        # ---------- JDK 17 (project requirement) ----------
        jdk = pkgs.jdk17;

      in
      {
        devShells.default = pkgs.mkShell {
          name = "bai-viet-game";

          buildInputs = with pkgs; [
            # ── Core toolchain ──
            jdk
            gradle
            kotlin

            # ── Android SDK ──
            androidSdk

            # ── Linting & quality ──
            ktlint
            detekt

            # ── Utilities ──
            git
            which
            coreutils
            findutils
            gnugrep
            gnused
            curl
            unzip
            zip
          ];

          # ── Environment variables ──
          JAVA_HOME      = "${jdk}";
          ANDROID_HOME   = "${androidSdk}/libexec/android-sdk";
          ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
          # NixOS: AAPT2 tải từ Maven là binary liên kết động, không chạy được → dùng aapt2 của SDK
          GRADLE_OPTS    = "-Dorg.gradle.java.home=${jdk} -Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/35.0.0/aapt2";

          shellHook = ''
            echo ""
            echo "╔══════════════════════════════════════════════════════════╗"
            echo "║  🎴  Bài Việt – Dev Environment                        ║"
            echo "╠══════════════════════════════════════════════════════════╣"
            echo "║  JDK:         $(java -version 2>&1 | head -1)"
            echo "║  Kotlin:      $(kotlin -version 2>&1 | head -1)"
            echo "║  Gradle:      $(gradle --version 2>/dev/null | grep '^Gradle' || echo 'use ./gradlew')"
            echo "║  Android SDK: $ANDROID_HOME"
            echo "║  ktlint:      $(ktlint --version 2>/dev/null || echo 'available')"
            echo "║  detekt:      $(detekt --version 2>/dev/null || echo 'available')"
            echo "╚══════════════════════════════════════════════════════════╝"
            echo ""
            echo "💡 Tip: dùng ./gradlew thay cho gradle hệ thống để đảm bảo version."
            echo ""

            # Add Android platform-tools & cmdline-tools to PATH
            export PATH="${androidSdk}/libexec/android-sdk/platform-tools:$PATH"
            export PATH="${androidSdk}/libexec/android-sdk/cmdline-tools/latest/bin:$PATH"
          '';
        };
      }
    );
}

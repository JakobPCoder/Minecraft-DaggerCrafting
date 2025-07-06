"""
build.py - Build the Fabric mod and copy the output JAR to the Minecraft mods folder.

- Runs the Gradle build.
- Determines the correct JAR filename from gradle.properties.
- Copies the JAR to the target mods folder.

Usage: python build.py
"""
import os
import shutil
import subprocess
import sys

# === GLOBALS ===
TARGET_MODS_FOLDER = r"C:\Users\Jonas\AppData\Roaming\.minecraft\mods"
PROPERTIES_FILE = "gradle.properties"
BUILD_LIBS_DIR = os.path.join("build", "libs")


def parse_gradle_properties(filepath):
    """
    Parse gradle.properties and return a dict of key-value pairs.
    """
    props = {}
    with open(filepath, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                k, v = line.split('=', 1)
                props[k.strip()] = v.strip()
    return props


def get_jar_filename(props):
    """
    Construct the expected JAR filename from gradle.properties values.
    """
    base = props.get("archives_base_name")
    version = props.get("mod_version")
    if not base or not version:
        raise ValueError("archives_base_name or mod_version not found in gradle.properties")
    return f"{base}-{version}.jar"


def run_gradle_build():
    """
    Run the Gradle build using the appropriate wrapper for the OS.
    """
    gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
    result = subprocess.run([gradlew, "build"], check=False)
    if result.returncode != 0:
        raise RuntimeError("Gradle build failed")


def copy_jar_to_mods(jar_path, target_folder):
    """
    Copy the built JAR to the Minecraft mods folder.
    """
    os.makedirs(target_folder, exist_ok=True)
    dest = os.path.join(target_folder, os.path.basename(jar_path))
    shutil.copy2(jar_path, dest)
    print(f"Copied {jar_path} to {dest}")


def main():
    """
    Main build and copy logic.
    """
    props = parse_gradle_properties(PROPERTIES_FILE)
    jar_filename = get_jar_filename(props)
    jar_path = os.path.join(BUILD_LIBS_DIR, jar_filename)

    print(f"Building project with Gradle...")
    run_gradle_build()

    if not os.path.isfile(jar_path):
        print(f"ERROR: JAR not found: {jar_path}", file=sys.stderr)
        sys.exit(1)

    print(f"Copying {jar_filename} to Minecraft mods folder...")
    copy_jar_to_mods(jar_path, TARGET_MODS_FOLDER)
    print("Done.")


if __name__ == "__main__":
    main() 
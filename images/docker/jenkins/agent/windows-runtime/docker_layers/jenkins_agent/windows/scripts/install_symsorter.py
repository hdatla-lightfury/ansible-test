import os
import subprocess
import shutil
import sys

# Constants
REPO_URL = "https://github.com/getsentry/symbolicator.git"
SYMBOLICATOR_LOCAL_PATH="C:\\workspace\\tools"

# TODO: https://lightfurygames.atlassian.net/browse/TITAN-2460
# we have to move our common utilities to github 
# and install as pip library here
# Like Command Handler and  logger in this case,
# until then, we have to live with this 
def run_command(command, cwd=None):
    """Executes a shell command in a subprocess.

    Args:
        command (str): The shell command to execute.
        cwd (str, optional): The working directory to run the command in. Defaults to None.

    Returns:
        None

    Raises:
        SystemExit: If the command returns a non-zero exit code, indicating failure.
    """
    result = subprocess.run(command, cwd=cwd, shell=True)
    if result.returncode != 0:
        print(f"Command failed: {command}")
        sys.exit(1)


def build_symsorter():
    """Builds symsorter from source code.

    Clones the symbolicator repository from GitHub, builds the symsorter binary using Cargo,
    and verifies the build output.

    Returns:
        None

    Raises:
        SystemExit: If cloning fails, symsorter directory is not found, build fails,
            or the final executable is not found.
    """
    
    clone_dir = os.path.join(SYMBOLICATOR_LOCAL_PATH, "symbolicator")

    # Delete existing clone if present
    if os.path.exists(clone_dir):
        print(f"Removing existing directory: {clone_dir}")
        shutil.rmtree(clone_dir)

    print(f"Cloning {REPO_URL} into {clone_dir}...")
    run_command(f"git clone {REPO_URL} {clone_dir}")

    symsorter_dir = os.path.join(clone_dir, "crates", "symsorter")

    if not os.path.exists(symsorter_dir):
        print("Could not find symsorter directory.")
        sys.exit(1)

    print("Building symsorter with cargo...")
    run_command("cargo build --release", cwd=symsorter_dir)

    exe_path = os.path.join(symsorter_dir, "target", "release", "symsorter.exe")
    if os.path.exists(exe_path):
        print(f"symsorter built successfully at: {exe_path}")
    else:
        print("symsorter.exe not found after build.")
        sys.exit(1)

if __name__ == "__main__":
    build_symsorter()

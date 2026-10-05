#!/usr/bin/env python3
"""Start the local backend using the same env file as setup-db.py."""
import argparse
import importlib.util
import os
from pathlib import Path
import shutil
import sys

spec = importlib.util.spec_from_file_location("setup_db", Path(__file__).with_name("setup-db.py"))
setup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(setup)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path)
    parser.add_argument("--jar", action="store_true", help="Run the built jar instead of Maven")
    args = parser.parse_args()
    path = args.env_file or setup.ROOT / ".env"
    if args.env_file and not path.is_file():
        parser.error(f"Env file does not exist: {path}")
    try:
        values = setup.settings(path)
        environment = os.environ.copy()
        if path.exists():
            environment.update({key: value for key, value in setup.read_env(path).items()
                                if key not in os.environ})
        environment.update({key: values[key] for key in ("DB_NAME", "DB_USERNAME", "DB_PASSWORD", "DB_HOST", "DB_PORT")})
        for key in ("DB_URL", "DB_ADMIN_PASSWORD", "DB_ADMIN_USERNAME", "DB_SOCKET", "MYSQL_ROOT_PASSWORD"):
            environment.pop(key, None)
        if args.jar:
            java = shutil.which("java")
            jar = setup.ROOT / "target/nspay-0.0.1-SNAPSHOT.jar"
            if not java or not jar.is_file():
                raise ValueError("Install Java and build the backend first with mvn package")
            os.chdir(setup.ROOT)
            os.execve(java, [java, "-jar", str(jar)], environment)
        maven = shutil.which("mvn")
        if not maven:
            raise ValueError("Install Maven and make mvn available on PATH")
        os.chdir(setup.ROOT)
        os.execve(maven, [maven, "spring-boot:run"], environment)
    except (ValueError, OSError) as error:
        print(f"Startup failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())

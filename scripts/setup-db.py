#!/usr/bin/env python3
"""Provision MySQL and import seed.sql using an env file (Python standard library only)."""
import argparse
import getpass
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parent.parent
DEFAULTS = {
    "DB_NAME": "payments", "DB_USERNAME": "payadmin", "DB_PASSWORD": "paypassword",
    "DB_HOST": "127.0.0.1", "DB_PORT": "3306", "DB_USER_HOST": "localhost",
    "DB_ADMIN_USERNAME": "root",
}


def read_env(path):
    """Read literal KEY=value pairs; never execute shell code or expand variables."""
    values = {}
    for number, raw in enumerate(path.read_text().splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        key, sep, value = line.partition("=")
        key, value = key.strip(), value.strip()
        if not sep or not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", key):
            raise ValueError(f"Invalid env entry on line {number}")
        if value.startswith(("'", '"')):
            if len(value) < 2 or value[-1] != value[0]:
                raise ValueError(f"Unclosed quote on env line {number}")
            value = value[1:-1]
        else:
            value = re.split(r"\s+#", value, maxsplit=1)[0].rstrip()
        values[key] = value
    return values


def settings(path):
    values = dict(DEFAULTS)
    if path.exists():
        values.update(read_env(path))
    for key in set(DEFAULTS) | {"DB_ADMIN_PASSWORD", "DB_SOCKET"}:
        if key in os.environ:
            values[key] = os.environ[key]
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,63}", values["DB_NAME"]):
        raise ValueError("DB_NAME must start with a letter and contain only letters, digits, or underscores (max 64)")
    if not values["DB_PORT"].isdigit() or not 1 <= int(values["DB_PORT"]) <= 65535:
        raise ValueError("DB_PORT must be between 1 and 65535")
    for key in ("DB_USERNAME", "DB_PASSWORD", "DB_HOST", "DB_USER_HOST", "DB_ADMIN_USERNAME"):
        if not values[key] or any(ord(char) < 32 for char in values[key]):
            raise ValueError(f"{key} must be nonempty and contain no control characters")
    if len(values["DB_USERNAME"]) > 32:
        raise ValueError("DB_USERNAME cannot exceed 32 characters")
    return values


def sql_literal(value):
    # seed.sql enables NO_BACKSLASH_ESCAPES before using these literals.
    return "'" + value.replace("'", "''") + "'"


def render_seed(values):
    name = values["DB_NAME"]
    replacements = {
        "DATABASE": "`" + name + "`",
        "GRANT_DATABASE": "`" + name.replace("_", "\\_") + "`",
        "USERNAME": sql_literal(values["DB_USERNAME"]),
        "PASSWORD": sql_literal(values["DB_PASSWORD"]),
        "USER_HOST": sql_literal(values["DB_USER_HOST"]),
    }
    template = (ROOT / "seed.sql").read_text()
    return re.sub(r"\{\{([A-Z_]+)\}\}", lambda match: replacements[match[1]], template)


def option_value(value):
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r") + '"'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, help="Env file (default: project .env, if present)")
    parser.add_argument("--dry-run", action="store_true", help="Validate configuration without connecting or displaying passwords")
    args = parser.parse_args()
    path = args.env_file or ROOT / ".env"
    if args.env_file and not path.is_file():
        parser.error(f"Env file does not exist: {path}")
    try:
        values = settings(path)
        sql = render_seed(values)
        if args.dry_run:
            print(f"Valid configuration: {values['DB_NAME']} on {values['DB_HOST']}:{values['DB_PORT']}; app user {values['DB_USERNAME']}@{values['DB_USER_HOST']}")
            return 0
        mysql = shutil.which("mysql")
        if not mysql:
            raise ValueError("Install the MySQL command-line client and make mysql available on PATH")
        password = values.get("DB_ADMIN_PASSWORD")
        if password is None:
            password = getpass.getpass(f"MySQL administrator password for {values['DB_ADMIN_USERNAME']}: ")
        # A private option file keeps the administrator password out of process arguments.
        with tempfile.TemporaryDirectory(prefix="nspay-db-") as directory:
            options = Path(directory) / "client.cnf"
            fields = {"user": values["DB_ADMIN_USERNAME"], "password": password}
            if values.get("DB_SOCKET"):
                connection = ["--protocol=SOCKET", f"--socket={values['DB_SOCKET']}"]
            else:
                connection = [f"--host={values['DB_HOST']}", f"--port={values['DB_PORT']}", "--protocol=TCP"]
            options.write_text("[client]\n" + "".join(f"{key}={option_value(value)}\n" for key, value in fields.items()))
            options.chmod(0o600)
            environment = os.environ.copy()
            environment.pop("MYSQL_PWD", None)
            result = subprocess.run([mysql, f"--defaults-file={options}", *connection, "--default-character-set=utf8mb4", "--batch"], input=sql, text=True,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=environment)
        if result.returncode:
            # Account-management errors can contain SQL: don't echo credentials.
            codes = re.findall(r"ERROR \d+ \([A-Z0-9]+\)(?: at line \d+)?", result.stderr)
            print("Database setup failed" + (": " + ", ".join(codes) if codes else "")
                  + ". Check administrator access, privileges, and schema compatibility.", file=sys.stderr)
            return 1
        print(f"Demo ready in {values['DB_NAME']}. Application database user: {values['DB_USERNAME']}.")
        return 0
    except (ValueError, OSError, EOFError) as error:
        print(f"Setup failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())

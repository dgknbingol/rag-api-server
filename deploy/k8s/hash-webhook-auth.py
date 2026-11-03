#!/usr/bin/env python3
import hashlib
import sys

v = sys.stdin.read().rstrip("\n").rstrip("\r")
print(f"len={len(v)} sha256={hashlib.sha256(v.encode()).hexdigest()}")

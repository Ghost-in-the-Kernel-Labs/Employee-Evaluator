"""The judge harness (phase 2, TODO.md 2.2): Python 3, standard library only (E-001, E-003, E-081).

Judgment lives here, outside the kernel, and reaches it only as facts with evidence (E-017). Nothing in this
package reaches the network or reads a key except `transport.py` (2.2l, E-087), the one door; `guard.py`
and its test hold that.
"""

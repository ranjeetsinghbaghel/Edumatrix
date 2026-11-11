import logging

attendance_logger = logging.getLogger("attendance")
attendance_logger.setLevel(logging.INFO)

handler = logging.StreamHandler()
formatter = logging.Formatter(
    "\n📋 ATTENDANCE MARKED\n"
    "------------------------------------------------\n"
    "ID  Name     Subject     Time       Period\n"
    "%(message)s\n"
    "------------------------------------------------"
)

handler.setFormatter(formatter)
attendance_logger.addHandler(handler)
attendance_logger.propagate = False

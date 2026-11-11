from datetime import datetime, time

CLASS_PERIODS = [
    {
        "period": "Period-0",
        "subject": "Testing",
        "start": time(6, 0),
        "end": time(8, 0)
    },
    {
        "period": "Period-1",
        "subject": "English",
        "start": time(8, 0),
        "end": time(9, 0)
    },
    {
        "period": "Period-2",
        "subject": "Technical",
        "start": time(9, 0),
        "end": time(10, 0)
    },
    {
        "period": "Period-3",
        "subject": "DBMS",
        "start": time(10, 0),
        "end": time(11, 0)
    },
    {
        "period": "Period-4",
        "subject": "OS",
        "start": time(11, 1),
        "end": time(12, 0)
    },
    {
        "period": "Period-5",
        "subject": "AADK",
        "start": time(14, 1),
        "end": time(15, 0)
    }
]

def get_current_period_and_subject():
    now = datetime.now().time()

    for p in CLASS_PERIODS:
        if p["start"] <= now <= p["end"]:
            return p["period"], p["subject"]

    return None, None



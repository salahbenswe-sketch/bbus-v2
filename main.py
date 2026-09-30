import os
import time
import json
import math
import base64
import socket
import logging
import threading
import requests
from bottle import Bottle, run, response, request
import firebase_admin
from firebase_admin import credentials, messaging

# Logging Configuration
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

# Environment Variables & Configuration
TRACCAR_BASE_URL = os.getenv("TRACCAR_BASE_URL", "https://demo.traccar.org")
TRACCAR_TOKEN = os.getenv("TRACCAR_TOKEN", "")
FCM_TOPIC = os.getenv("FCM_TOPIC", "bus_alerts")
POLL_INTERVAL_SECONDS = int(os.getenv("POLL_INTERVAL_SECONDS", "5"))
ALERT_RADIUS_METERS = float(os.getenv("ALERT_RADIUS_METERS", "100"))
ALERT_COOLDOWN_SECONDS = int(os.getenv("ALERT_COOLDOWN_SECONDS", "180"))  # 3 minutes
HTTP_TIMEOUT_SECONDS = int(os.getenv("HTTP_TIMEOUT_SECONDS", "10"))
COOLDOWN_FILE = os.getenv("COOLDOWN_FILE", "/data/cooldown_state.json")

# Mapping stationKey to exact raw MP3 sound name in app's res/raw/ folder
STATION_SOUNDS = {
    "DOURA": "doura",
    "BENI_ATLI": "bni_attali",
    "BENI_ATLI_RETURN": "bni_attali2",
    "ADAHMI": "adahmi",
    "ADAHMI_RETURN": "adahmi2",
    "BOUAMER": "bouamer",
    "BOUAMER_RETURN": "bouamer2",
    "EL_HANOUT": "elhanout",
    "CHRACHRIA": "chrachria",
    "CHRACHRIA_RETURN": "chrachria2",
    "BELHADRI": "belhadri",
    "BELHADRI_RETURN": "belhadri2",
    "EL_FORKANE": "el_forkane"
}

# Mapping stationKey to the EXACT NotificationChannel id already created on the
# Android app (see AlertStation enum in BusAlertManager.kt). This must NOT be
# derived from station_key.lower(), because "BENI_ATLI".lower() == "beni_atli"
# which does not match the real channel id "bus_alert_bni_attali_v8". If the
# channel_id sent in the FCM payload does not match an existing channel on the
# device, Android silently drops the notification whenever the app is fully
# closed (killed) — this was the root cause of alerts not showing up.
STATION_CHANNELS = {
    "DOURA": "bus_alert_doura_v8",
    "BENI_ATLI": "bus_alert_bni_attali_v8",
    "BENI_ATLI_RETURN": "bus_alert_bni_attali_return_v8",
    "ADAHMI": "bus_alert_adahmi_v8",
    "ADAHMI_RETURN": "bus_alert_adahmi_return_v8",
    "BOUAMER": "bus_alert_bouamer_v8",
    "BOUAMER_RETURN": "bus_alert_bouamer_return_v8",
    "EL_HANOUT": "bus_alert_el_hanout_v8",
    "CHRACHRIA": "bus_alert_chrachria_v8",
    "CHRACHRIA_RETURN": "bus_alert_chrachria_return_v8",
    "BELHADRI": "bus_alert_belhadri_v8",
    "BELHADRI_RETURN": "bus_alert_belhadri_return_v8",
    "EL_FORKANE": "bus_alert_el_forkane_v8",
}

# Station Coordinates & Exact Android stationKey Mapping
STATIONS = [
    {
        "name": "مسجد الفرقان",
        "lat": 36.264338,
        "lon": 2.758888,
        "directions": {
            "outbound": "EL_FORKANE",
            "return": "EL_FORKANE"
        }
    },
    {
        "name": "بلحضري",
        "lat": 36.273830,
        "lon": 2.766270,
        "directions": {
            "outbound": "BELHADRI",
            "return": "BELHADRI_RETURN"
        }
    },
    {
        "name": "الشراشرية",
        "lat": 36.287951,
        "lon": 2.751746,
        "directions": {
            "outbound": "CHRACHRIA",
            "return": "CHRACHRIA_RETURN"
        }
    },
    {
        "name": "محطة الدورة",
        "lat": 36.288847,
        "lon": 2.749923,
        "directions": {
            "outbound": "DOURA"
        }
    },
    {
        "name": "محطة بني عطلي",
        "lat": 36.295291,
        "lon": 2.740911,
        "directions": {
            "outbound": "BENI_ATLI",
            "return": "BENI_ATLI_RETURN"
        }
    },
    {
        "name": "محطة الدهمي",
        "lat": 36.297794,
        "lon": 2.735820,
        "directions": {
            "outbound": "ADAHMI",
            "return": "ADAHMI_RETURN"
        }
    },
    {
        "name": "محطة بوعامر",
        "lat": 36.300976,
        "lon": 2.731803,
        "directions": {
            "outbound": "BOUAMER",
            "return": "BOUAMER_RETURN"
        }
    },
    {
        "name": "محطة الحانوت",
        "lat": 36.304553,
        "lon": 2.729345,
        "directions": {
            "outbound": "EL_HANOUT"
        }
    }
]

# --- DNS-over-HTTPS fallback -------------------------------------------
# WORKAROUND for a hosting-platform network issue: this container's DNS
# resolver cannot reach UDP:53 (confirmed via /diag), so every normal
# hostname lookup fails, even though outbound TCP egress works fine.
# When the system resolver fails, fall back to resolving the hostname via
# Cloudflare's DNS-over-HTTPS endpoint (queried over HTTPS/TCP:443, which
# we've confirmed is reachable), then retry with the resolved IP literal.
# This is a stopgap — the real fix is enabling UDP:53 egress on the
# Deplexo side. Remove this block once that's fixed.
_orig_getaddrinfo = socket.getaddrinfo
_doh_cache = {}

def _doh_resolve(hostname, timeout=5):
    url = "https://1.1.1.1/dns-query"
    headers = {"accept": "application/dns-json"}
    r = requests.get(url, params={"name": hostname, "type": "A"}, headers=headers, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    ips = [a["data"] for a in data.get("Answer", []) if a.get("type") == 1]
    if not ips:
        raise socket.gaierror(f"DNS-over-HTTPS returned no A records for {hostname}")
    return ips

def _patched_getaddrinfo(host, *args, **kwargs):
    try:
        return _orig_getaddrinfo(host, *args, **kwargs)
    except socket.gaierror:
        if host in _doh_cache:
            resolved_ip = _doh_cache[host]
        else:
            resolved_ip = _doh_resolve(host)[0]
            _doh_cache[host] = resolved_ip
            logging.warning(f"System DNS failed for '{host}'; resolved via DNS-over-HTTPS to {resolved_ip}.")
        return _orig_getaddrinfo(resolved_ip, *args, **kwargs)

socket.getaddrinfo = _patched_getaddrinfo
# --- end DNS-over-HTTPS fallback ----------------------------------------

# Last concrete reason Firebase init failed (for accurate diagnostics on /status and /test_alert).
_firebase_init_error = None

# Initialize Firebase Admin.
#
# SECURITY: credentials are NEVER hardcoded in this file. They are read at
# runtime from the FIREBASE_SERVICE_ACCOUNT_BASE64 environment variable
# (set in Deplexo), or from a local serviceAccountKey.json file for local
# development only (this file is gitignored and must never be committed).
def init_firebase():
    global _firebase_init_error

    if firebase_admin._apps:
        return True

    sa_b64 = os.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64", "").strip()
    if sa_b64:
        try:
            clean_b64 = "".join(sa_b64.split())
            cred_json = json.loads(base64.b64decode(clean_b64).decode("utf-8"))
            cred = credentials.Certificate(cred_json)
            firebase_admin.initialize_app(cred)
            project_id = cred_json.get("project_id", "unknown")
            logging.info(f"Firebase Admin initialized successfully for project: {project_id}")
            _firebase_init_error = None
            return True
        except Exception as e:
            _firebase_init_error = f"Failed to initialize Firebase from BASE64 env var: {e}"
            logging.exception(_firebase_init_error)
            return False

    if os.path.exists("serviceAccountKey.json"):
        try:
            cred = credentials.Certificate("serviceAccountKey.json")
            firebase_admin.initialize_app(cred)
            logging.info("Firebase Admin initialized from local serviceAccountKey.json.")
            _firebase_init_error = None
            return True
        except Exception as e:
            _firebase_init_error = f"Failed to initialize Firebase from serviceAccountKey.json: {e}"
            logging.exception(_firebase_init_error)
            return False

    _firebase_init_error = (
        "No Firebase credentials provided. Set FIREBASE_SERVICE_ACCOUNT_BASE64 "
        "in Deplexo, or place a serviceAccountKey.json file next to main.py for local runs."
    )
    logging.warning(_firebase_init_error)
    return False

# Cooldown Persistence across Container Restarts
last_alert_time = {}

def load_cooldown_state():
    global last_alert_time

    if not os.path.exists(COOLDOWN_FILE):
        logging.info(f"No cooldown state file yet: {COOLDOWN_FILE}")
        last_alert_time = {}
        return

    try:
        with open(COOLDOWN_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)

        if isinstance(data, dict):
            last_alert_time = data
            logging.info(f"Loaded {len(last_alert_time)} cooldown records from {COOLDOWN_FILE}.")
        else:
            logging.warning(f"Cooldown state is not a JSON object. Resetting: {COOLDOWN_FILE}")
            last_alert_time = {}
    except Exception as e:
        logging.error(f"Error loading cooldown state file: {e}")
        last_alert_time = {}

def save_cooldown_state():
    try:
        directory = os.path.dirname(os.path.abspath(COOLDOWN_FILE))
        if directory:
            os.makedirs(directory, exist_ok=True)

        temp_file = f"{COOLDOWN_FILE}.tmp"
        with open(temp_file, "w", encoding="utf-8") as f:
            json.dump(last_alert_time, f, ensure_ascii=False, indent=2)

        os.replace(temp_file, COOLDOWN_FILE)
    except Exception as e:
        logging.error(f"Error saving cooldown state: {e}")

def haversine_distance(lat1, lon1, lat2, lon2):
    R = 6371000.0 # Earth radius in meters
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (math.sin(dlat / 2.0) ** 2 +
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2.0) ** 2)
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    return R * c

def get_bus_direction(course, speed_kmh):
    if speed_kmh < 1.0 or course == 0.0:
        return "outbound"
    norm_course = (course % 360.0 + 360.0) % 360.0
    if 110.0 <= norm_course <= 250.0:
        return "return"
    return "outbound"

def send_fcm_alert(title, body, direction, station_name, station_key, bus_name="حافلة بني عطلي"):
    if not firebase_admin._apps:
        logging.warning(f"[SIMULATED FCM] Firebase is not initialized. title='{title}', body='{body}', stationKey='{station_key}'")
        return False, (_firebase_init_error or "Firebase Admin SDK is not initialized.")

    sound_name = STATION_SOUNDS.get(station_key, "bni_attali")
    channel_id = STATION_CHANNELS.get(station_key, "bus_alert_bni_attali_v8")

    try:
        # Passing exact raw sound_name instructs Android OS to play res/raw/<sound_name>.mp3
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data={
                "direction": direction,
                "station": station_name,
                "stationKey": station_key,
                "busName": bus_name,
                "title": title,
                "body": body
            },
            topic=FCM_TOPIC,
            android=messaging.AndroidConfig(
                priority="high",
                ttl=0,
                notification=messaging.AndroidNotification(
                    channel_id=channel_id,
                    sound=sound_name,
                    visibility="public"
                )
            )
        )
        response_id = messaging.send(message)
        logging.info(f"FCM Alert Sent! ID: {response_id}, StationKey: {station_key}, Sound: {sound_name}, Channel: {channel_id}")
        return True, None
    except Exception as e:
        error_msg = f"Failed to send FCM alert for stationKey {station_key}: {e}"
        logging.error(error_msg)
        return False, error_msg

def check_bus_positions():
    load_cooldown_state()

    headers = {}
    if TRACCAR_TOKEN:
        headers["Authorization"] = f"Bearer {TRACCAR_TOKEN}"
    else:
        logging.warning("TRACCAR_TOKEN is empty. Traccar requests will be sent without Authorization.")

    session = requests.Session()
    session.headers.update(headers)

    logging.info(f"Tracker started. Traccar={TRACCAR_BASE_URL}, poll={POLL_INTERVAL_SECONDS}s, radius={ALERT_RADIUS_METERS}m, cooldown={ALERT_COOLDOWN_SECONDS}s, cooldown_file={COOLDOWN_FILE}")

    while True:
        try:
            pos_resp = session.get(f"{TRACCAR_BASE_URL}/api/positions", timeout=HTTP_TIMEOUT_SECONDS)
            dev_resp = session.get(f"{TRACCAR_BASE_URL}/api/devices", timeout=HTTP_TIMEOUT_SECONDS)

            if pos_resp.status_code != 200 or dev_resp.status_code != 200:
                logging.error(f"Traccar HTTP error: positions={pos_resp.status_code}, devices={dev_resp.status_code}")
                time.sleep(POLL_INTERVAL_SECONDS)
                continue

            positions = pos_resp.json()
            devices = {d["id"]: d["name"] for d in dev_resp.json()}
            logging.info(f"Traccar OK: {len(positions)} position(s), {len(devices)} device(s)")

            now = time.time()

            for pos in positions:
                device_id = pos.get("deviceId")
                bus_name = devices.get(device_id, "حافلة بني عطلي")
                lat = pos.get("latitude")
                lon = pos.get("longitude")
                course = float(pos.get("course", 0.0) or 0.0)
                speed_knots = float(pos.get("speed", 0.0) or 0.0)
                speed_kmh = speed_knots * 1.852

                if lat is None or lon is None:
                    logging.warning(f"Skipping device {device_id}: missing latitude/longitude")
                    continue

                direction = get_bus_direction(course, speed_kmh)

                for station in STATIONS:
                    if direction not in station["directions"]:
                        continue

                    station_key = station["directions"][direction]
                    dist = haversine_distance(lat, lon, station["lat"], station["lon"])

                    if dist <= ALERT_RADIUS_METERS:
                        cooldown_key = f"{device_id}_{station_key}"
                        last_time = float(last_alert_time.get(cooldown_key, 0) or 0)

                        if now - last_time > ALERT_COOLDOWN_SECONDS:
                            dir_text = "اتجاه المدية" if direction == "return" else "اتجاه بني عطلي"
                            title = f"🚌 الحافلة اقتربت من {station['name']}!"
                            body = f"{bus_name} قادمة الآن ({dir_text}) - المسافة: {int(dist)} متر"

                            sent, _ = send_fcm_alert(
                                title=title, body=body, direction=direction,
                                station_name=station["name"], station_key=station_key,
                                bus_name=bus_name
                            )

                            # Start cooldown only after a successful FCM send.
                            if sent:
                                last_alert_time[cooldown_key] = now
                                save_cooldown_state()
                                logging.info(f"Cooldown saved for {cooldown_key}.")
                            else:
                                logging.warning(f"Alert was not sent; cooldown was NOT recorded for {cooldown_key}.")

        except requests.RequestException as e:
            logging.error(f"Traccar request error: {e}")
        except (ValueError, KeyError, TypeError) as e:
            logging.error(f"Invalid Traccar response/data: {e}")
        except Exception as e:
            logging.exception(f"Unexpected error in tracking loop: {e}")

        time.sleep(POLL_INTERVAL_SECONDS)


# Bottle Web Server for Deplexo Health Check & Direct Test Endpoints
app = Bottle()

@app.route("/health")
def health_check():
    response.content_type = "application/json"
    return json.dumps({"status": "ok", "app": "bni_attali_bus_tracker_v2", "firebase_initialized": bool(firebase_admin._apps)})

@app.route("/test_alert")
def test_alert_route():
    # Ensure Firebase is initialized even if this endpoint is called before startup initialization.
    init_firebase()

    station_key = request.query.get("station", "BENI_ATLI").strip().upper()
    sound_name = STATION_SOUNDS.get(station_key, "bni_attali")

    title = f"🚌 تجربة ديبليكسو: الحافلة اقتربت من محطة بني عطلي!"
    body = f"إشعار تجريبي صادر مباشرة من خادم ديبليكسو"

    success, error = send_fcm_alert(title, body, "outbound", "محطة بني عطلي", station_key)
    response.content_type = "application/json"
    if success:
        return json.dumps({"status": "success", "message": f"FCM alert sent to topic '{FCM_TOPIC}' with sound '{sound_name}.mp3'", "stationKey": station_key}, ensure_ascii=False)
    else:
        return json.dumps({"status": "failed", "error": error or "Unknown error while sending the FCM alert."}, ensure_ascii=False)

@app.route("/diag")
def diag_route():
    # Network/DNS diagnostic: checks whether this container can resolve and
    # reach a few external hosts. Helps tell apart "Google is blocked" from
    # "this container has no outbound network access at all".
    response.content_type = "application/json"
    hosts_to_check = [
        "oauth2.googleapis.com",
        "fcm.googleapis.com",
        "www.google.com",
        "1.1.1.1",  # Cloudflare, IP literal — resolves instantly, tests raw TCP egress
    ]
    try:
        traccar_host = TRACCAR_BASE_URL.split("//")[-1].split("/")[0].split(":")[0]
        if traccar_host:
            hosts_to_check.append(traccar_host)
    except Exception:
        pass

    results = {}
    for host in hosts_to_check:
        try:
            ip = socket.gethostbyname(host)
            results[host] = {"dns_ok": True, "resolved_ip": ip}
        except Exception as e:
            results[host] = {"dns_ok": False, "error": str(e)}

    # Raw TCP egress test, bypassing DNS entirely, to tell apart
    # "DNS resolver is broken" from "there is no outbound network at all".
    tcp_targets = [("1.1.1.1", 443), ("8.8.8.8", 53)]
    tcp_results = {}
    for ip, port in tcp_targets:
        key = f"{ip}:{port}"
        try:
            with socket.create_connection((ip, port), timeout=5):
                tcp_results[key] = {"tcp_ok": True}
        except Exception as e:
            tcp_results[key] = {"tcp_ok": False, "error": str(e)}

    doh_result = {}
    try:
        ip = _doh_resolve("oauth2.googleapis.com")
        doh_result = {"doh_ok": True, "resolved_ip": ip}
    except Exception as e:
        doh_result = {"doh_ok": False, "error": str(e)}

    return json.dumps({"dns_checks": results, "raw_tcp_checks": tcp_results, "dns_over_https_check": doh_result}, ensure_ascii=False)

@app.route("/status")
def status_route():
    response.content_type = "application/json"
    firebase_active = bool(firebase_admin._apps)
    return json.dumps({
        "firebase_initialized": firebase_active,
        "firebase_init_error": _firebase_init_error,
        "fcm_topic": FCM_TOPIC,
        "traccar_url": TRACCAR_BASE_URL,
        "alert_radius_meters": ALERT_RADIUS_METERS,
        "alert_cooldown_seconds": ALERT_COOLDOWN_SECONDS,
        "active_cooldowns_count": len(last_alert_time)
    }, ensure_ascii=False)

if __name__ == "__main__":
    logging.info("Starting BNI Attali Bus Tracker...")
    init_firebase()

    # Start detection loop in a background daemon thread
    tracker_thread = threading.Thread(target=check_bus_positions, daemon=True)
    tracker_thread.start()

    # Run Bottle HTTP server
    port = int(os.getenv("PORT", "8080"))
    logging.info(f"HTTP health server starting on 0.0.0.0:{port}")
    run(app, host="0.0.0.0", port=port)

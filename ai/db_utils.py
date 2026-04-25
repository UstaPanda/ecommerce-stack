import os
import psycopg2
from psycopg2 import pool
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv
import logging
import time
import re

load_dotenv()

logger = logging.getLogger("DB-Utils")

DB_URL = os.getenv("DB_URL")
if not DB_URL:
    raise ValueError("DB_URL environment variable is not set")

# Log the host being used (without credentials) to help diagnose connection issues
try:
    _host = re.search(r"@([^/]+)/", DB_URL).group(1)
    logger.info(f"DB_URL host resolved to: {_host}")
except Exception:
    logger.info(f"DB_URL loaded (could not parse host)")

_db_pool = None


def _get_pool():
    """Lazy pool initializer with retry logic."""
    global _db_pool
    if _db_pool is not None:
        return _db_pool

    retries = 5
    for attempt in range(1, retries + 1):
        try:
            _db_pool = psycopg2.pool.SimpleConnectionPool(1, 20, DB_URL)
            logger.info("Database connection pool initialized successfully.")
            return _db_pool
        except Exception as e:
            logger.warning(f"DB connection attempt {attempt}/{retries} failed: {e}")
            if attempt < retries:
                time.sleep(3)
            else:
                logger.error(f"Could not initialize DB pool after {retries} attempts.")
    return None


def get_db_connection():
    """Get a connection from the pool."""
    p = _get_pool()
    if p:
        return p.getconn()
    return None


def release_db_connection(conn):
    """Return a connection to the pool."""
    p = _get_pool()
    if p and conn:
        p.putconn(conn)


def execute_sql(query: str) -> list:
    """Execute a SQL query in read-only mode and return standard dictionaries."""
    forbidden_keywords = [
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
        "TRUNCATE", "GRANT", "REVOKE", "MERGE", "CALL", "EXECUTE"
    ]
    query_upper = query.strip().upper()

    if ";" in query.strip()[:-1]:
        return {"error": "Güvenlik kısıtlaması: Çoklu komut yürütmeye izin verilmiyor."}

    if not (query_upper.startswith("SELECT") or query_upper.startswith("WITH")):
        return {"error": "Güvenlik kısıtlaması: Sadece veri okuma (SELECT) işlemlerine izin verilmektedir."}

    for word in forbidden_keywords:
        if re.search(rf"\b{word}\b", query_upper):
            return {"error": f"Güvenlik kısıtlaması: '{word}' işlemi gerçekleştirilemez."}

    conn = get_db_connection()
    if not conn:
        return {"error": "Veritabanına bağlanılamadı."}

    try:
        conn.set_session(readonly=True, autocommit=True)
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(query)
            if cur.description:
                results = cur.fetchall()
                return [dict(row) for row in results]
            else:
                return [{"message": "İşlem tamamlandı, ancak veri dönmedi."}]
    except Exception as e:
        logger.error(f"SQL Execution Error: {e} | Query: {query}")
        return {"error": str(e)}
    finally:
        try:
            conn.set_session(readonly=False, autocommit=False)
        except Exception:
            pass
        release_db_connection(conn)


def get_user_by_email(email: str):
    """Retrieve user id and role_type by email for JWT compliance."""
    conn = get_db_connection()
    if not conn:
        return None
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("SELECT id, role_type FROM users WHERE email = %s", (email,))
            result = cur.fetchone()
            return dict(result) if result else None
    except Exception as e:
        logger.error(f"Error looking up user by email: {e}")
        return None
    finally:
        release_db_connection(conn)


def get_schema_info():
    """Retrieve the schema info AND sample values for categorical columns."""
    tables = [
        "users", "customer_profiles", "stores", "products",
        "categories", "orders", "order_items", "shipments", "reviews",
        "carts", "cart_items"
    ]

    schema_info = "DATABASE SCHEMA AND SAMPLE DATA:\n"
    conn = get_db_connection()
    if not conn:
        return "Error: Could not retrieve schema info."

    try:
        with conn.cursor() as cur:
            for table in tables:
                cur.execute(f"""
                    SELECT column_name, data_type
                    FROM information_schema.columns
                    WHERE table_name = '{table}' AND table_schema = 'public'
                    ORDER BY ordinal_position;
                """)
                columns = cur.fetchall()
                schema_info += f"\nTable: {table}\n"

                for col_name, data_type in columns:
                    sample_str = ""
                    if data_type in ['character varying', 'text', 'character']:
                        try:
                            cur.execute(f"SELECT DISTINCT {col_name} FROM {table} WHERE {col_name} IS NOT NULL LIMIT 3;")
                            samples = [str(r[0]) for r in cur.fetchall()]
                            if samples:
                                sample_str = f" | Samples: {', '.join(samples)}"
                        except Exception:
                            pass

                    schema_info += f"  - {col_name} ({data_type}){sample_str}\n"

        return schema_info
    except Exception as e:
        logger.error(f"Error retrieving schema info: {e}")
        return f"Error retrieving schema info: {e}"
    finally:
        release_db_connection(conn)


if __name__ == "__main__":
    print(get_schema_info())

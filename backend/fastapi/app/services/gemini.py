import os
from typing import Any

from dotenv import load_dotenv
from google import genai

load_dotenv()

_api_key = os.getenv("GEMINI_API_KEY")
_model = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")

_client = None


def _get_client():
    """Lazy 초기화 — GEMINI_API_KEY 없으면 호출 시점에 에러 발생"""
    global _client
    if _client is None:
        if not _api_key:
            raise RuntimeError("GEMINI_API_KEY is not set.")
        _client = genai.Client(api_key=_api_key)
    return _client


def analyze_diet(diets: list[dict[str, Any]]) -> str:
    """식단 데이터를 받아 Gemini로 한국어 분석 결과를 반환한다."""
    prompt = f"""
너는 건강관리 앱의 식단 분석 AI야.
아래 사용자의 식단 데이터를 보고 한국어로 짧고 친절하게 분석해줘.

조건:
- 의학적 진단처럼 말하지 말 것
- 개선점은 3개 이내로 말할 것
- 사용자가 바로 실천할 수 있게 말할 것
- 답변은 5문장 이내로 작성할 것

식단 데이터:
{diets}
"""
    client = _get_client()
    response = client.models.generate_content(
        model=_model,
        contents=prompt,
    )
    return response.text or ""

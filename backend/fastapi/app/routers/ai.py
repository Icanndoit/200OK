from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.services.gemini import analyze_diet


# prefix 제거: Spring Boot의 AiAnalysisClient가 /analyze/diet 로 호출하기 때문
router = APIRouter(tags=["ai"])


class DietAnalyzeRequest(BaseModel):
    diets: list[dict[str, Any]]


class AnalyzeResponse(BaseModel):
    answer: str


@router.post("/analyze/diet", response_model=AnalyzeResponse)
def post_analyze_diet(request: DietAnalyzeRequest) -> AnalyzeResponse:
    """Spring Boot의 AiAnalysisClient가 호출하는 엔드포인트"""
    if not request.diets:
        return AnalyzeResponse(answer="분석할 식단 데이터가 아직 없어요.")

    try:
        answer = analyze_diet(request.diets)
    except RuntimeError as exc:
        # GEMINI_API_KEY 미설정 등
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail="Gemini API request failed.") from exc

    if not answer:
        raise HTTPException(status_code=502, detail="Gemini API response is empty.")

    return AnalyzeResponse(answer=answer)


# === 추후 구현 예정 ===


@router.post("/ai/predict/blood-glucose/{user_id}")
async def predict_blood_glucose(user_id: str):
    pass


@router.post("/ai/analyze/pain/{user_id}")
async def analyze_pain(user_id: str):
    pass

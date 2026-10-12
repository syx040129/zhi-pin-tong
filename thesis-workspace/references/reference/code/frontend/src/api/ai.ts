import request from '@/utils/request'
import type {
  AiAnalysisVO,
  ChatAskRequest,
  ChatAskVO,
  ChatGreetingVO,
  ChatHistoryVO,
  ChatSessionVO,
  InterviewAnswerRequest,
  InterviewReportVO,
  InterviewStartRequest,
  InterviewVO,
  MatchRequest,
  MatchResultVO,
  OptimizeRequest,
  OptimizeResultVO,
  RecommendItemVO,
} from '@/types/api'

/* ===== AI 简历分析 / 优化 ===== */

export function analyzeResume(resumeId: number): Promise<AiAnalysisVO> {
  return request.post<AiAnalysisVO>(`/ai/resume/analyze/${resumeId}`)
}

export function latestAnalysis(resumeId: number): Promise<AiAnalysisVO> {
  return request.get<AiAnalysisVO>(`/ai/resume/analysis/${resumeId}`)
}

export function optimizeResume(payload: OptimizeRequest): Promise<OptimizeResultVO> {
  return request.post<OptimizeResultVO>('/ai/resume/optimize', payload)
}

/* ===== AI 岗位匹配 / 推荐 ===== */

export function matchJob(payload: MatchRequest): Promise<MatchResultVO> {
  return request.post<MatchResultVO>('/ai/match', payload)
}

export function getRecommendations(): Promise<RecommendItemVO[]> {
  return request.get<RecommendItemVO[]>('/ai/recommendations')
}

export function refreshRecommendations(): Promise<RecommendItemVO[]> {
  return request.post<RecommendItemVO[]>('/ai/recommendations/refresh')
}

/* ===== AI 模拟面试 ===== */

export function interviewStart(payload: InterviewStartRequest): Promise<InterviewVO> {
  return request.post<InterviewVO>('/ai/interview/start', payload)
}

export function interviewList(): Promise<InterviewVO[]> {
  return request.get<InterviewVO[]>('/ai/interview/list')
}

export function interviewDetail(id: number): Promise<InterviewVO> {
  return request.get<InterviewVO>(`/ai/interview/${id}`)
}

export function interviewAnswer(
  id: number,
  payload: InterviewAnswerRequest,
): Promise<InterviewVO['questions'][number]> {
  return request.post<InterviewVO['questions'][number]>(`/ai/interview/${id}/answer`, payload)
}

export function interviewFinish(id: number): Promise<InterviewReportVO> {
  return request.post<InterviewReportVO>(`/ai/interview/${id}/finish`)
}

export function interviewReport(id: number): Promise<InterviewReportVO> {
  return request.get<InterviewReportVO>(`/ai/interview/${id}/report`)
}

/* ===== AI 智能助手（右下角悬浮对话框） ===== */

export function chatGreeting(): Promise<ChatGreetingVO> {
  return request.get<ChatGreetingVO>('/ai/chat/greeting')
}

export function chatAsk(payload: ChatAskRequest): Promise<ChatAskVO> {
  return request.post<ChatAskVO>('/ai/chat/ask', payload)
}

export function chatHistory(): Promise<ChatHistoryVO> {
  return request.get<ChatHistoryVO>('/ai/chat/history')
}

export function chatSessions(): Promise<ChatSessionVO[]> {
  return request.get<ChatSessionVO[]>('/ai/chat/sessions')
}

export function chatClear(): Promise<void> {
  return request.delete<void>('/ai/chat/history')
}

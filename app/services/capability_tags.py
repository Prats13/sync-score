"""
Central registry of capability tags, dependency mappings, and scoring weights.
Every service imports from here — never define tags in multiple places.
"""

# ── All valid tags ────────────────────────────────────────────────────────────
ALL_TAGS: list[str] = [
    "rag",
    "vector_search",
    "agent_systems",
    "multi_agent",
    "tool_orchestration",
    "memory_systems",
    "voice_ai",
    "speech_recognition",
    "text_to_speech",
    "computer_vision",
    "object_detection",
    "gesture_recognition",
    "llm_integration",
    "deep_learning",
    "machine_learning",
    "hardware_integration",
    "iot",
    "api_development",
    "data_pipeline",
    "async_processing",
    "deployment",
    "audio_processing",
    "nlp",
    "document_processing",
]

# ── Scoring weights ───────────────────────────────────────────────────────────
TAG_WEIGHTS: dict[str, int] = {
    "rag": 12,
    "vector_search": 8,
    "agent_systems": 12,
    "multi_agent": 8,
    "tool_orchestration": 8,
    "memory_systems": 8,
    "voice_ai": 10,
    "speech_recognition": 6,
    "text_to_speech": 4,
    "computer_vision": 8,
    "object_detection": 6,
    "gesture_recognition": 4,
    "llm_integration": 10,
    "deep_learning": 8,
    "machine_learning": 6,
    "hardware_integration": 5,
    "iot": 4,
    "api_development": 5,
    "data_pipeline": 5,
    "async_processing": 3,
    "deployment": 4,
    "audio_processing": 4,
    "nlp": 6,
    "document_processing": 4,
}

# ── Python package → tags ─────────────────────────────────────────────────────
PYTHON_DEP_TAGS: dict[str, list[str]] = {
    # RAG / Vector Search
    "langchain": ["rag", "tool_orchestration", "llm_integration"],
    "langchain-core": ["rag", "tool_orchestration", "llm_integration"],
    "langchain-community": ["rag", "tool_orchestration", "llm_integration"],
    "llama-index": ["rag", "vector_search", "llm_integration"],
    "llamaindex": ["rag", "vector_search", "llm_integration"],
    "llama_index": ["rag", "vector_search", "llm_integration"],
    "chromadb": ["vector_search", "rag"],
    "pinecone-client": ["vector_search", "rag"],
    "pinecone": ["vector_search", "rag"],
    "faiss-cpu": ["vector_search", "rag"],
    "faiss-gpu": ["vector_search", "rag"],
    "faiss": ["vector_search", "rag"],
    "weaviate-client": ["vector_search", "rag"],
    "qdrant-client": ["vector_search", "rag"],
    "pymilvus": ["vector_search", "rag"],
    "sentence-transformers": ["rag", "nlp", "llm_integration"],
    # Agent Systems
    "langgraph": ["agent_systems", "tool_orchestration"],
    "autogen": ["agent_systems", "multi_agent"],
    "pyautogen": ["agent_systems", "multi_agent"],
    "crewai": ["agent_systems", "multi_agent"],
    "semantic-kernel": ["agent_systems", "tool_orchestration", "llm_integration"],
    "agentops": ["agent_systems"],
    "pydantic-ai": ["agent_systems", "llm_integration"],
    # Memory Systems
    "mem0ai": ["memory_systems"],
    "mem0": ["memory_systems"],
    "zep-python": ["memory_systems"],
    "zep": ["memory_systems"],
    # Voice AI
    "openai-whisper": ["voice_ai", "speech_recognition"],
    "whisper": ["voice_ai", "speech_recognition"],
    "speechbrain": ["voice_ai", "speech_recognition"],
    "pyaudio": ["voice_ai", "audio_processing"],
    "librosa": ["voice_ai", "audio_processing"],
    "pyttsx3": ["voice_ai", "text_to_speech"],
    "gtts": ["voice_ai", "text_to_speech"],
    "elevenlabs": ["voice_ai", "text_to_speech"],
    "deepgram": ["voice_ai", "speech_recognition"],
    "assemblyai": ["voice_ai", "speech_recognition"],
    "azure-cognitiveservices-speech": ["voice_ai", "speech_recognition", "text_to_speech"],
    "google-cloud-speech": ["voice_ai", "speech_recognition"],
    "sounddevice": ["voice_ai", "audio_processing"],
    "pydub": ["voice_ai", "audio_processing"],
    "faster-whisper": ["voice_ai", "speech_recognition"],
    "silero-vad": ["voice_ai", "speech_recognition"],
    # LiveKit voice AI plugins
    "livekit-agents": ["voice_ai", "agent_systems"],
    "livekit-plugins-deepgram": ["voice_ai", "speech_recognition"],
    "livekit-plugins-assemblyai": ["voice_ai", "speech_recognition"],
    "livekit-plugins-elevenlabs": ["voice_ai", "text_to_speech"],
    "livekit-plugins-cartesia": ["voice_ai", "text_to_speech"],
    "livekit-plugins-openai": ["voice_ai", "llm_integration"],
    "livekit-plugins-anthropic": ["voice_ai", "llm_integration"],
    "livekit-plugins-google": ["voice_ai", "llm_integration"],
    "livekit-plugins-groq": ["voice_ai", "llm_integration"],
    "livekit-plugins-aws": ["voice_ai", "speech_recognition", "text_to_speech"],
    "livekit-plugins-silero": ["voice_ai", "speech_recognition"],
    "livekit-plugins-noise-cancellation": ["voice_ai", "audio_processing"],
    "livekit-plugins-hume": ["voice_ai", "text_to_speech"],
    "livekit-plugins-rime": ["voice_ai", "text_to_speech"],
    "livekit-plugins-sarvam": ["voice_ai", "speech_recognition", "text_to_speech"],
    "livekit-plugins-turn-detector": ["voice_ai", "agent_systems"],
    # Computer Vision
    "mediapipe": ["computer_vision", "gesture_recognition"],
    "opencv-python": ["computer_vision"],
    "opencv-python-headless": ["computer_vision"],
    "ultralytics": ["computer_vision", "object_detection"],
    "torchvision": ["computer_vision", "deep_learning"],
    "pillow": ["computer_vision"],
    # LLM Integration
    "openai": ["llm_integration"],
    "anthropic": ["llm_integration"],
    "google-generativeai": ["llm_integration"],
    "google-genai": ["llm_integration"],
    "cohere": ["llm_integration"],
    "huggingface-hub": ["llm_integration", "deep_learning"],
    "transformers": ["llm_integration", "deep_learning", "nlp"],
    "mistralai": ["llm_integration"],
    "groq": ["llm_integration"],
    "together": ["llm_integration"],
    "replicate": ["llm_integration"],
    # ML / Deep Learning
    "torch": ["deep_learning"],
    "tensorflow": ["deep_learning"],
    "keras": ["deep_learning"],
    "scikit-learn": ["machine_learning"],
    "xgboost": ["machine_learning"],
    "lightgbm": ["machine_learning"],
    "catboost": ["machine_learning"],
    # NLP
    "spacy": ["nlp"],
    "nltk": ["nlp"],
    "gensim": ["nlp"],
    "textblob": ["nlp"],
    "tokenizers": ["nlp", "llm_integration"],
    # Hardware / IoT
    "pyserial": ["hardware_integration", "iot"],
    "gpiozero": ["hardware_integration", "iot"],
    "pyfirmata": ["hardware_integration", "iot"],
    "pyfirmata2": ["hardware_integration", "iot"],
    "pymata4": ["hardware_integration", "iot"],
    "pymata-express": ["hardware_integration", "iot"],
    "pyduino": ["hardware_integration", "iot"],
    "pushbullet.py": ["iot"],
    "pushbullet": ["iot"],
    "face-recognition": ["computer_vision", "object_detection"],
    "face_recognition": ["computer_vision", "object_detection"],
    "deepface": ["computer_vision", "object_detection"],
    "imutils": ["computer_vision"],
    "dlib": ["computer_vision", "object_detection"],
    # API / Deployment
    "fastapi": ["api_development"],
    "flask": ["api_development"],
    "django": ["api_development"],
    "aiohttp": ["api_development", "async_processing"],
    # Data Pipeline
    "celery": ["data_pipeline", "async_processing"],
    "kafka-python": ["data_pipeline", "async_processing"],
    "prefect": ["data_pipeline"],
    # Document Processing
    "pdfplumber": ["document_processing"],
    "pymupdf": ["document_processing"],
    "pypdf2": ["document_processing"],
    "python-docx": ["document_processing"],
}

# ── JavaScript/Node package → tags ────────────────────────────────────────────
JS_DEP_TAGS: dict[str, list[str]] = {
    "langchain": ["rag", "tool_orchestration", "llm_integration"],
    "@langchain/core": ["rag", "tool_orchestration", "llm_integration"],
    "openai": ["llm_integration"],
    "@anthropic-ai/sdk": ["llm_integration"],
    "chromadb": ["vector_search", "rag"],
    "@pinecone-database/pinecone": ["vector_search", "rag"],
    "qdrant-client": ["vector_search", "rag"],
    "@tensorflow/tfjs": ["deep_learning"],
    "natural": ["nlp"],
    "express": ["api_development"],
    "fastify": ["api_development"],
    "next": ["api_development"],
    "socket.io": ["async_processing"],
    "bull": ["data_pipeline", "async_processing"],
    "bullmq": ["data_pipeline", "async_processing"],
}


def deps_to_tags(packages: list[str], dep_map: dict[str, list[str]]) -> list[str]:
    """Map a list of package names to deduplicated capability tags."""
    tags: set[str] = set()
    for pkg in packages:
        pkg_lower = pkg.lower().strip()
        if pkg_lower in dep_map:
            tags.update(dep_map[pkg_lower])
    return sorted(tags)

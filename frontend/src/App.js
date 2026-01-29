import React, { useEffect, useState, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import ErrorMessage from './ErrorMessage';

function App() {
  const [message, setMessage] = useState('');
  const [chatHistory, setChatHistory] = useState([]);
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionID, setSessionID] = useState('');
  const [copyAlert, setCopyAlert] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [error, setError] = useState(null);

  const chatEndRef = useRef(null);

  //load or create sessionid and chat history
  useEffect(() => {
    const initSession = async () => {
      let id = localStorage.getItem("sessionId");

      if (!id) {
        id = crypto.randomUUID();
        localStorage.setItem("sessionId", id);
      }

      setSessionID(id);

      try {
        const res = await fetch(
          `http://localhost:8080/api/chat/history/${id}`
        );
        if (res.ok) {
          const history = await res.json();
          setChatHistory(history);
        }
      } catch (err) {
        console.error('Failed to load history:', err);
      } finally {
        setLoadingHistory(false);
      }
    };

    initSession();
  }, []);



  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatHistory, response]);


  const handleNewConversation = (e) => {
    e.preventDefault()
    const newId = crypto.randomUUID();
    localStorage.setItem('sessionId', newId);
    setSessionID(newId);
    setChatHistory([]);
    setMessage('');
    setError(null);
  };

  const handleCopy = (content) => {
    navigator.clipboard.writeText(content);
    setCopyAlert(true);
    setTimeout(() => setCopyAlert(false), 2000);
  };


  const handleDismissError = () => {
    setError(null);
  };


  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!message.trim()) return;

    const userMessage = message;
    const timestamp = new Date().toISOString();
    setMessage('');
    setLoading(true);
    setResponse('');
    setError(null);

    setChatHistory(prev => [...prev, {
      role: "user",
      content: userMessage,
      timestamp
    }]);

    try {
      const res = await fetch('http://localhost:8080/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMessage, sessionID })
      });

      // Handle HTTP errors
      if (!res.ok) {
        const errorData = await res.json().catch(() => ({ message: `Error ${res.status}` }));
        setError({ message: errorData.message });
        setLoading(false);
        return;
      }

      // Stream response
      const reader = res.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let buffer = '';
      let assistantMessage = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split("\n\n");
        buffer = events.pop() || "";

        for (const event of events) {
          const lines = event.split("\n");
          for (const line of lines) {
            if (!line.startsWith("data:")) continue;
            let token = line.slice(5);
            if (token === "") token = "\n";
            assistantMessage += token;
            setResponse(assistantMessage);
          }
        }
      }

      if (buffer.startsWith("data:")) {
        assistantMessage += buffer.slice(5);
        setResponse(assistantMessage);
      }

      setChatHistory(prev => [...prev, {
        role: "assistant",
        content: assistantMessage
      }]);
      setResponse('');

    } catch (error) {
      setError({ message: "Cannot connect to server. Check your connection." });
    }

    setLoading(false);
  };

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loadingHistory) {
    return <div className="loading">Loading conversation...</div>;
  }
  
  {error && (
  <div style={{ background: 'red', color: 'white', padding: 10 }}>
    ERROR STATE: {error.message}
  </div>
)}

  return (
    <div className="app-container">
      {copyAlert && <div className="copy-alert">Copied to clipboard!</div>}

      <div className="header">
        <h1>Finance Assistant AI</h1>
        <button onClick={handleNewConversation} className="new-chat-btn">
          New Conversation
        </button>
      </div>

      <ErrorMessage
        error={error}
        onDismiss={handleDismissError}
      />

      <div className="chat-container">
        {chatHistory.map((msg, index) => (
          <div key={index} className={`message ${msg.role}`}>
            <div className="message-header">
              <strong className="message-sender">
                {msg.role === 'user' ? 'You' : 'AI Finance'}
              </strong>
              {msg.role === 'user' && msg.timestamp && (
                <span className="timestamp">{formatTime(msg.timestamp)}</span>
              )}
            </div>
            <div className="message-content">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {msg.content}
              </ReactMarkdown>
            </div>
            {msg.role === 'assistant' && (
              <button
                onClick={() => handleCopy(msg.content)}
                className="copy-btn"
              >
                Copy
              </button>
            )}
          </div>
        ))}

        {response && (
          <div className="message assistant streaming">
            <div className="message-header">
              <strong className="message-sender">AI Finance</strong>
            </div>
            <div className="message-content">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {response}
              </ReactMarkdown>
            </div>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      <form onSubmit={handleSubmit} className="input-form">
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask a finance question..."
          className="message-input"
          disabled={loading}
        />
        <button
          type="submit"
          disabled={loading || !message.trim()}
          className="send-btn"
        >
          {loading ? 'Thinking...' : 'Send'}
        </button>
      </form>
    </div>
  );
}

export default App;
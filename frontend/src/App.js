import React, { useEffect, useState, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

function App() {
  const [message, setMessage] = useState('');
  const [chatHistory, setChatHistory] = useState([]); //
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionID, setSessionID] = useState('');
  const [copyAlert, setCopyAlert] = useState(false);

  const chatEndRef = useRef(null);

  useEffect(() => {
    setSessionID(crypto.randomUUID());
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatHistory, response]);

  const handleNewConversation = (e) => {
    e.preventDefault()
    setSessionID(crypto.randomUUID());
    setChatHistory([]);
    setMessage('');
  };

  const handleCopy = (content) => {
    navigator.clipboard.writeText(content);
    setCopyAlert(true);
    setTimeout(() => setCopyAlert(false), 2000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!message.trim()) return;

    const userMessage = message;
    const timestamp = new Date().toISOString();
    setMessage('');
    setLoading(true);
    setResponse('');

    //user message to the chat
    setChatHistory(prev => [...prev, { role: "user", content: userMessage, timestamp: timestamp }]);

    try {
      const res = await fetch('http://localhost:8080/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, sessionID })
      });

      const reader = res.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let buffer = '';  // Buffer for incomplete lines
      let assistantMessage = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split("\n\n");
        buffer = events.pop() || "";

        for (const event of events) {
          const lines = event.split("\n")

          for (const line of lines) {
            if (!line.startsWith("data:")) continue;
            let token = line.slice(5);
            if (token === "") token = "\n"; 
            assistantMessage += token;
            setResponse(assistantMessage);
          }
        }
      }

      // flush leftover
      if (buffer.startsWith("data:")) {
        const token = buffer.slice(5)
        assistantMessage += token
        setResponse(assistantMessage);
      }

      //add complete response to the chat history
      setChatHistory(prev => [...prev, { role: "assistant", content: assistantMessage }]);
      setResponse('')
    } catch (error) {
      setChatHistory(prev => [...prev, {
        role: 'assistant',
        content: `Error: ${error.message}`
      }]);
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

  return (
    <div className="app-container">
      {copyAlert && <div className="copy-alert">Copied to clipboard!</div>}
      
      <div className="header">
        <h1>Finance Assistant AI</h1>
        <button onClick={handleNewConversation} className="new-chat-btn">
          New Conversation
        </button>
      </div>

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
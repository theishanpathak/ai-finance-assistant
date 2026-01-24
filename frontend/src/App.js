import React, { useEffect, useState, useRef } from 'react';
import ReactMarkDown from 'react-markdown';

function App() {
  const [message, setMessage] = useState('');
  const [chatHistory, setChatHistory] = useState([]); //
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionID, setSessionID] = useState('')

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

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!message.trim()) return;

    const userMessage = message;
    setMessage('');
    setLoading(true);
    setResponse('');

    //user message to the chat
    setChatHistory(prev => [...prev, { role: "user", content: userMessage }]);

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
        buffer = events.pop();

        for (const event of events) {
          const lines = event.split("\n")

          for (const line of lines) {
            if (!line.startsWith("data:")) continue;
            const token = line.slice(5);
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

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h1>Finance Assistant AI</h1>
        <button onClick={handleNewConversation} style={{ padding: '8px 16px' }}>
          New Conversation
        </button>
      </div>

      {/* Chat History */}
      <div style={{
        maxHeight: '500px',
        overflowY: 'auto',
        marginBottom: '20px',
        border: '1px solid #ddd',
        borderRadius: '8px',
        padding: '16px',
        backgroundColor: '#fafafa'
      }}>
        {chatHistory.map((msg, index) => (
          <div key={index} style={{
            marginBottom: '16px',
            padding: '12px',
            borderRadius: '8px',
            backgroundColor: msg.role === 'user' ? '#e3f2fd' : '#f5f5f5',
            border: msg.role === 'user' ? '1px solid #2196f3' : '1px solid #ddd'
          }}>
            <strong style={{ color: msg.role === 'user' ? '#1976d2' : '#424242' }}>
              {msg.role === 'user' ? 'You' : 'AI Tutor'}:
            </strong>
            <div style={{ marginTop: '8px' }}>
              <ReactMarkDown>{msg.content}</ReactMarkDown>
            </div>
          </div>
        ))}

        {/* Streaming Response */}
        {response && (
          <div style={{
            marginBottom: '16px',
            padding: '12px',
            borderRadius: '8px',
            backgroundColor: '#f5f5f5',
            border: '1px solid #ddd'
          }}>
            <strong style={{ color: '#424242' }}>AI Tutor:</strong>
            <div style={{ marginTop: '8px' }}>
              <ReactMarkDown>{response}</ReactMarkDown>
            </div>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      {/* Input Form */}
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '8px' }}>
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask a finance question..."
          style={{
            flex: 1,
            padding: '12px',
            fontSize: '14px',
            border: '1px solid #ddd',
            borderRadius: '4px'
          }}
          disabled={loading}
        />
        <button
          type="submit"
          disabled={loading || !message.trim()}
          style={{
            padding: '12px 24px',
            backgroundColor: loading ? '#ccc' : '#2196f3',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '14px'
          }}
        >
          {loading ? 'Thinking...' : 'Send'}
        </button>
      </form>
    </div>
  );
}

export default App;
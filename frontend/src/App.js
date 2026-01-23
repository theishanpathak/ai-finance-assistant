import React, { useState } from 'react';
import ReactMarkDown from 'react-markdown';

function App() {
  const [message, setMessage] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResponse('');

    try {
      const res = await fetch('http://localhost:8080/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message })
      });

      const reader = res.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let buffer = '';  // Buffer for incomplete lines

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

            setResponse(prev => prev + token);
          }
        } 
      }
      
      // flush leftover
        if (buffer.startsWith("data:")) {
          setResponse((prev) => prev + buffer.slice(5));
        }
    } catch (error) {
      setResponse('Error: ' + error.message);
    }

    setLoading(false);
  };

  return (
    <div className="App">
      <h1>Finance AI Tutor</h1>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask a finance question..."
          style={{ width: '400px', padding: '10px' }}
          disabled={loading}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Thinking...' : 'Ask'}
        </button>
      </form>

      {response && (
        <div style={{ marginTop: '20px', padding: '20px', background: '#f0f0f0' }}>
          <strong>Response:</strong>
          <div style={{
            whiteSpace: 'pre-wrap',
            fontFamily: 'inherit',
            margin: 0,
            wordWrap: 'break-word'
          }}>
            <ReactMarkDown>{response}</ReactMarkDown>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
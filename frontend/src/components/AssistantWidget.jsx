import { useState } from 'react';
import { assistant } from '../api/client';

export default function AssistantWidget() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Hi! Ask me about items, pricing, or an order status.' },
  ]);
  const [loading, setLoading] = useState(false);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    setMessages((m) => [...m, { role: 'user', text }]);
    setInput('');
    setLoading(true);
    try {
      const { data } = await assistant.post('/api/assistant/chat', { message: text });
      const meta = data.tools_used?.length
        ? ` (tools: ${data.tools_used.join(', ')} · ${data.mode})`
        : ` (${data.mode})`;
      setMessages((m) => [...m, { role: 'assistant', text: data.answer + meta }]);
    } catch (e) {
      setMessages((m) => [...m, { role: 'assistant', text: 'Assistant is unavailable right now.' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <button
        onClick={() => setOpen((o) => !o)}
        className="fixed bottom-6 right-6 h-14 w-14 rounded-full bg-brand text-2xl text-white shadow-lg hover:bg-brand-dark"
        aria-label="Toggle AI assistant"
      >
        💬
      </button>

      {open && (
        <div className="fixed bottom-24 right-6 flex h-96 w-80 flex-col rounded-xl bg-white shadow-2xl">
          <div className="rounded-t-xl bg-brand px-4 py-2 font-semibold text-white">
            AI Trading Assistant
          </div>
          <div className="flex-1 space-y-2 overflow-y-auto p-3 text-sm">
            {messages.map((m, i) => (
              <div key={i} className={m.role === 'user' ? 'text-right' : 'text-left'}>
                <span
                  className={
                    'inline-block max-w-[85%] rounded-lg px-3 py-1.5 ' +
                    (m.role === 'user' ? 'bg-brand text-white' : 'bg-slate-100 text-slate-800')
                  }
                >
                  {m.text}
                </span>
              </div>
            ))}
            {loading && <div className="text-slate-400">thinking…</div>}
          </div>
          <div className="flex gap-2 border-t p-2">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && send()}
              placeholder="e.g. status of order #1"
              className="flex-1 rounded border px-2 py-1 text-sm focus:border-brand focus:outline-none"
            />
            <button
              onClick={send}
              className="rounded bg-brand px-3 py-1 text-sm text-white hover:bg-brand-dark"
            >
              Send
            </button>
          </div>
        </div>
      )}
    </>
  );
}

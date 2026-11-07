import React from 'react'
import ReactDOM from 'react-dom/client'
import './style.css'

function App() {
  return <main><h1>TicketFlow 用户端</h1><p>工程已就绪。下一步接入演出列表与购票流程。</p></main>
}

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>)

import React from 'react'
import ReactDOM from 'react-dom/client'
import { Card, Layout, Typography } from 'antd'

function App() {
  return <Layout style={{ minHeight: '100vh', padding: 48 }}><Card><Typography.Title>TicketFlow 主办方端</Typography.Title><Typography.Paragraph>工程已就绪。下一步接入演出与场次管理。</Typography.Paragraph></Card></Layout>
}

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>)

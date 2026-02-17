import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

// FIX: Polyfill pentru 'global' necesar pentru stompjs.
// Îl punem aici pentru a fi siguri că rulează înainte de orice altceva.
if (typeof global === 'undefined') {
  window.global = window;
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

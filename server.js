const http = require('http');

const PORT = 3000;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.url === '/health' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', serverSecretConfigured: !!GEMINI_API_KEY }));
    return;
  }

  if ((req.url === '/api/podcast/generate' || req.url === '/api/generate-podcast') && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk.toString(); });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const pdfTitle = payload.pdfTitle || 'Document';
        const pdfText = payload.pdfText || '';

        if (!GEMINI_API_KEY) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ isSuccess: false, errorMessage: 'Clé d\'API Gemini non configurée sur le serveur backend.' }));
          return;
        }

        if (!pdfText || pdfText.trim().length < 50) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ isSuccess: false, errorMessage: 'Texte insuffisant pour la génération.' }));
          return;
        }

        const scriptPrompt = `
Tu es un concepteur pédagogique expert et un podcasteur chevronné.
À partir du cours PDF intitulé "${pdfTitle}", rédige un court épisode de podcast pédagogique sous forme de dialogue entre deux personnes :
1. PROFESSEUR (expert, bienveillant, structuré, qui explique clairement les concepts clés).
2. ELEVE (curieux, motivé, pose des questions pertinentes et résume en ses propres mots).

Consignes impératives :
- Langue : Français naturel, fluide et captivant.
- Format : Réponds STRICTEMENT avec un tableau JSON d'objets sans aucun texte additionnel ni balise markdown autour, sous la forme :
[
  {"speaker": "PROFESSEUR", "text": "Bonjour et bienvenue dans notre épisode révision sur ${pdfTitle} !"},
  {"speaker": "ELEVE", "text": "Bonjour professeur ! Quels sont les points essentiels à retenir aujourd'hui ?"}
]
- Génère entre 6 et 10 répliques équilibrées et claires.

Voici le contenu du PDF :
${pdfText.slice(0, 12000)}
        `.trim();

        const scriptUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;
        const scriptResp = await fetch(scriptUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: scriptPrompt }] }],
            generationConfig: { temperature: 0.7 }
          })
        });

        if (!scriptResp.ok) {
          const errText = await scriptResp.text();
          throw new Error(`Erreur Gemini Script: ${scriptResp.status} ${errText}`);
        }

        const scriptData = await scriptResp.json();
        let rawText = scriptData.candidates?.[0]?.content?.parts?.[0]?.text || '';
        rawText = rawText.trim().replace(/^```json/i, '').replace(/^```/i, '').replace(/```$/i, '').trim();

        const scriptLines = JSON.parse(rawText);
        const audioChunks = [];

        const ttsUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=${GEMINI_API_KEY}`;
        for (const line of scriptLines) {
          const voiceName = line.speaker === 'PROFESSEUR' ? 'Puck' : 'Fenrir';
          const ttsResp = await fetch(ttsUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [{ parts: [{ text: line.text }] }],
              generationConfig: {
                responseModalities: ['AUDIO'],
                speechConfig: {
                  voiceConfig: {
                    prebuiltVoiceConfig: { voiceName }
                  }
                }
              }
            })
          });

          if (ttsResp.ok) {
            const ttsData = await ttsResp.json();
            const b64 = ttsData.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
            if (b64) {
              audioChunks.push(b64);
            }
          }
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          isSuccess: true,
          scriptJson: JSON.stringify(scriptLines),
          audioChunksBase64: audioChunks
        }));

      } catch (err) {
        console.error('Server error generating podcast:', err);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          isSuccess: false,
          errorMessage: 'Erreur backend : ' + err.message
        }));
      }
    });
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
});

server.listen(PORT, () => {
  console.log(`Secure Gemini Backend Server running on port ${PORT}`);
});

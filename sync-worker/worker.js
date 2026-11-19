/**
 * Optional self-hosted Cloudflare Worker + KV relay for BunkerStock Family Sync.
 * Stores only opaque AES-256-GCM encrypted blobs — zero plaintext ever touches the worker.
 */
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const cors = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
    };
    if (request.method === 'OPTIONS') return new Response(null, { headers: cors });

    if (request.method === 'POST' && (url.pathname === '/objects' || url.pathname === '/')) {
      const id = crypto.randomUUID().replace(/-/g, '');
      const body = await request.text();
      await env.BUNKER_KV.put(id, body);
      return new Response(JSON.stringify({ id }), {
        headers: { ...cors, 'Content-Type': 'application/json' },
      });
    }

    const id = url.pathname.split('/').filter(Boolean).pop();
    if (!id) return new Response('Not found', { status: 404, headers: cors });

    if (request.method === 'GET') {
      const val = await env.BUNKER_KV.get(id);
      if (!val) return new Response('Not found', { status: 404, headers: cors });
      return new Response(val, { headers: { ...cors, 'Content-Type': 'application/json' } });
    }

    if (request.method === 'PUT') {
      const body = await request.text();
      await env.BUNKER_KV.put(id, body);
      return new Response(JSON.stringify({ id, ok: true }), {
        headers: { ...cors, 'Content-Type': 'application/json' },
      });
    }

    return new Response('Method not allowed', { status: 405, headers: cors });
  },
};

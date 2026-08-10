## 2026-08-10
- 移除首頁對傳統慈善/非營利夥伴的酸言酸語(原本『legacy systems extracting』『not just a donation』等字句聽起來像在貶低捐款制傳統慈善模式),重寫成『和現有的社區組織、教會、非營利夥伴一起把基礎建設往前推進』的夥伴關係語氣,STYLE_GUIDE.md 加了語氣守則避免以後再犯 (sess-20260809-1420-e9d3d7f8)
- Contact Us 改成真的表單,送到 IDUNA 新的 /api/v1/carepyre/contact 端點(移除 hello@carepyre.org mailto 連結),nginx 加上 /api/ proxy 到 IDUNA(:8080),與 okemily.com 同樣模式 (sess-20260809-1420-e9d3d7f8)

- feat(site): landing page + deployment prep for carepyre.org. Rebuilt index.html against STYLE_GUIDE.md's new direction (light blue/white/cyan/orange, healthcare-friendly, not the dark EINHORN_INDUSTRIAL aesthetic) after the founder redirected away from the first dark-theme draft. Content covers the mission, the 4-layer C.H.A.N.G.E. stack, and the Trust/Foundation/Co-op structure -- deliberately excludes the Centene/Medicaid-MCO-displacement competitive strategy and specific capitation figures from the source material, per explicit founder instruction. Added ops/nginx-carepyre.conf + sudo-queue/13-carepyre-domain-setup.sh (same pattern as okemily.com's own standup) -- Claude has no sudo access on this box, so the actual domain/cert setup is queued for the founder to run. (sess-20260809-1420-e9d3d7f8)


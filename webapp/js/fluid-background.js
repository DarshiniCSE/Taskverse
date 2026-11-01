/**
 * TaskVerse — Interactive Purple Fluid Canvas Background
 * High-performance ambient fluid simulation with organic cursor interaction.
 * Color Palette: Deep Midnight Purple (#0B0714), Vibrant Violet (#8B5CF6),
 * Bright Lavender (#C4B5FD), Soft Purple (#A78BFA), Neon Magenta (#D946EF).
 */
(function() {
    'use strict';

    // Wait until DOM is ready
    function initFluid() {
        const canvas = document.getElementById('fluidCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        // Check for reduced motion preference
        const prefersReducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        let width = 0;
        let height = 0;
        let dpr = 1;

        // Mouse / Pointer State
        const pointer = {
            x: -2000,
            y: -2000,
            targetX: -2000,
            targetY: -2000,
            radius: 300,
            active: false,
            strength: 0,
            lastMoved: 0
        };

        // Ambient fluid nodes
        const blobs = [
            {
                baseX: 0.18, baseY: 0.22,
                radius: 360,
                freqX: 0.0005, freqY: 0.0007,
                ampX: 130, ampY: 90,
                phase: 0.2,
                colorStops: [
                    { pos: 0.0, color: 'rgba(139, 92, 246, 0.30)' }, // Vibrant Violet
                    { pos: 0.45, color: 'rgba(107, 63, 160, 0.16)' },
                    { pos: 0.85, color: 'rgba(26, 16, 43, 0.04)' },
                    { pos: 1.0, color: 'rgba(8, 6, 16, 0)' }
                ]
            },
            {
                baseX: 0.82, baseY: 0.28,
                radius: 400,
                freqX: 0.00045, freqY: 0.00065,
                ampX: 150, ampY: 110,
                phase: 1.9,
                colorStops: [
                    { pos: 0.0, color: 'rgba(196, 181, 253, 0.22)' }, // Bright Lavender
                    { pos: 0.40, color: 'rgba(167, 139, 250, 0.14)' },
                    { pos: 0.80, color: 'rgba(107, 63, 160, 0.04)' },
                    { pos: 1.0, color: 'rgba(8, 6, 16, 0)' }
                ]
            },
            {
                baseX: 0.50, baseY: 0.72,
                radius: 440,
                freqX: 0.0006, freqY: 0.00045,
                ampX: 170, ampY: 100,
                phase: 3.4,
                colorStops: [
                    { pos: 0.0, color: 'rgba(107, 63, 160, 0.32)' }, // Deep Midnight Purple
                    { pos: 0.50, color: 'rgba(38, 24, 59, 0.18)' },
                    { pos: 0.85, color: 'rgba(11, 7, 20, 0.05)' },
                    { pos: 1.0, color: 'rgba(8, 6, 16, 0)' }
                ]
            },
            {
                baseX: 0.12, baseY: 0.80,
                radius: 320,
                freqX: 0.00075, freqY: 0.00055,
                ampX: 110, ampY: 120,
                phase: 4.7,
                colorStops: [
                    { pos: 0.0, color: 'rgba(217, 70, 239, 0.20)' }, // Neon Magenta Highlight
                    { pos: 0.40, color: 'rgba(139, 92, 246, 0.12)' },
                    { pos: 0.80, color: 'rgba(26, 16, 43, 0.03)' },
                    { pos: 1.0, color: 'rgba(8, 6, 16, 0)' }
                ]
            },
            {
                baseX: 0.88, baseY: 0.82,
                radius: 380,
                freqX: 0.00055, freqY: 0.0008,
                ampX: 140, ampY: 130,
                phase: 5.8,
                colorStops: [
                    { pos: 0.0, color: 'rgba(139, 92, 246, 0.22)' }, // Soft Violet
                    { pos: 0.50, color: 'rgba(107, 63, 160, 0.12)' },
                    { pos: 0.85, color: 'rgba(8, 6, 16, 0.02)' },
                    { pos: 1.0, color: 'rgba(8, 6, 16, 0)' }
                ]
            }
        ];

        // Spring displacement states for each node
        const blobStates = blobs.map(() => ({
            x: 0, y: 0,
            dispX: 0, dispY: 0
        }));

        function handleResize() {
            dpr = Math.min(window.devicePixelRatio || 1, 2);
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = Math.floor(width * dpr);
            canvas.height = Math.floor(height * dpr);
            canvas.style.width = width + 'px';
            canvas.style.height = height + 'px';
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

            // If reduced motion, re-render static snapshot
            if (prefersReducedMotion) {
                renderFrame(performance.now());
            }
        }

        window.addEventListener('resize', handleResize, { passive: true });
        handleResize();

        // Pointer event listeners (desktop mouse + tablet/mobile touch)
        window.addEventListener('pointermove', function(e) {
            pointer.targetX = e.clientX;
            pointer.targetY = e.clientY;
            pointer.lastMoved = performance.now();
            if (!pointer.active) {
                pointer.active = true;
                pointer.x = e.clientX;
                pointer.y = e.clientY;
            }
        }, { passive: true });

        window.addEventListener('pointerleave', function() {
            pointer.active = false;
        }, { passive: true });

        // Loop controls
        let animationFrameId = null;
        let isTabActive = true;

        document.addEventListener('visibilitychange', function() {
            if (document.hidden) {
                isTabActive = false;
                if (animationFrameId) cancelAnimationFrame(animationFrameId);
            } else {
                isTabActive = true;
                if (!prefersReducedMotion) {
                    animationFrameId = requestAnimationFrame(loop);
                }
            }
        });

        function renderFrame(now) {
            ctx.clearRect(0, 0, width, height);

            // Smooth pointer easing (lerp)
            if (pointer.active) {
                pointer.x += (pointer.targetX - pointer.x) * 0.07;
                pointer.y += (pointer.targetY - pointer.y) * 0.07;
                pointer.strength = Math.min(pointer.strength + 0.04, 1);
            } else {
                pointer.strength = Math.max(pointer.strength - 0.02, 0);
            }

            // Render ambient fluid blobs
            for (let i = 0; i < blobs.length; i++) {
                const b = blobs[i];
                const s = blobStates[i];

                // Harmonic ambient drift
                const targetX = b.baseX * width + Math.sin(now * b.freqX + b.phase) * b.ampX;
                const targetY = b.baseY * height + Math.cos(now * b.freqY + b.phase) * b.ampY;

                // Fluid magnetic interaction with cursor
                if (pointer.strength > 0.01) {
                    const dx = targetX - pointer.x;
                    const dy = targetY - pointer.y;
                    const dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < pointer.radius * 2 && dist > 1) {
                        const force = (1 - dist / (pointer.radius * 2)) * 65 * pointer.strength;
                        s.dispX += ((dx / dist) * force - s.dispX) * 0.08;
                        s.dispY += ((dy / dist) * force - s.dispY) * 0.08;
                    } else {
                        s.dispX += (0 - s.dispX) * 0.04;
                        s.dispY += (0 - s.dispY) * 0.04;
                    }
                } else {
                    s.dispX += (0 - s.dispX) * 0.04;
                    s.dispY += (0 - s.dispY) * 0.04;
                }

                s.x = targetX + s.dispX;
                s.y = targetY + s.dispY;

                // Draw radial fluid blob
                const rad = b.radius;
                const grad = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, rad);
                for (let k = 0; k < b.colorStops.length; k++) {
                    const stop = b.colorStops[k];
                    grad.addColorStop(stop.pos, stop.color);
                }

                ctx.fillStyle = grad;
                ctx.beginPath();
                ctx.arc(s.x, s.y, rad, 0, Math.PI * 2);
                ctx.fill();
            }

            // Interactive cursor fluid aura
            if (pointer.strength > 0.02) {
                const cursorRadius = pointer.radius * 0.85;
                const cursorGrad = ctx.createRadialGradient(
                    pointer.x, pointer.y, 0,
                    pointer.x, pointer.y, cursorRadius
                );
                cursorGrad.addColorStop(0.0, 'rgba(167, 139, 250, ' + (0.20 * pointer.strength) + ')');
                cursorGrad.addColorStop(0.35, 'rgba(139, 92, 246, ' + (0.12 * pointer.strength) + ')');
                cursorGrad.addColorStop(0.70, 'rgba(217, 70, 239, ' + (0.05 * pointer.strength) + ')');
                cursorGrad.addColorStop(1.0, 'rgba(8, 6, 16, 0)');

                ctx.fillStyle = cursorGrad;
                ctx.beginPath();
                ctx.arc(pointer.x, pointer.y, cursorRadius, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        function loop(now) {
            if (!isTabActive) return;
            renderFrame(now);
            if (!prefersReducedMotion) {
                animationFrameId = requestAnimationFrame(loop);
            }
        }

        if (!prefersReducedMotion) {
            animationFrameId = requestAnimationFrame(loop);
        } else {
            renderFrame(performance.now());
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initFluid);
    } else {
        initFluid();
    }
})();

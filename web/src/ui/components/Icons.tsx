interface IconeProps {
  tamanho?: number;
  className?: string;
}

const base = (tamanho: number) => ({
  width: tamanho,
  height: tamanho,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
});

export const IconeVolei = ({ tamanho = 22 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 3c-2.2 5.5-2.2 12.5 0 18" />
    <path d="M4 7.5c5 3 11 3 16 0" />
    <path d="M4 16.5c5-3 11-3 16 0" />
  </svg>
);

export const IconeTrofeu = ({ tamanho = 22 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M7 4h10v5a5 5 0 0 1-10 0V4z" />
    <path d="M7 6H4v1a3 3 0 0 0 3 3" />
    <path d="M17 6h3v1a3 3 0 0 1-3 3" />
    <path d="M9 20h6M12 14v6" />
  </svg>
);

export const IconeGrupos = ({ tamanho = 22 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M16 20v-1.5a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4V20" />
    <circle cx="9" cy="7" r="3.5" />
    <path d="M22 20v-1.5a4 4 0 0 0-3-3.87" />
    <path d="M16 3.6a4 4 0 0 1 0 7.3" />
  </svg>
);

export const IconePessoa = ({ tamanho = 18 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

export const IconePessoaFora = ({ tamanho = 18 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M15 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
    <circle cx="8.5" cy="7" r="4" />
    <path d="M17 8l5 5M22 8l-5 5" />
  </svg>
);

export const IconeSair = ({ tamanho = 18 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
    <path d="M16 17l5-5-5-5M21 12H9" />
  </svg>
);

export const IconeNuvemOk = ({ tamanho = 15 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M18 17h-.5a4.5 4.5 0 0 0-1.2-8.8 6 6 0 0 0-11.3 2A4 4 0 0 0 6 17h1" />
    <path d="M9 15l2.5 2.5L16 13" />
  </svg>
);

export const IconeNuvemSync = ({ tamanho = 15 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M18 17h-.5a4.5 4.5 0 0 0-1.2-8.8 6 6 0 0 0-11.3 2A4 4 0 0 0 6 17h1" />
    <path d="M9.5 15.5A3 3 0 0 0 15 14M15 11a3 3 0 0 0-5.5 1.5" />
  </svg>
);

export const IconeNuvemOff = ({ tamanho = 15 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M18.6 17.4A4.5 4.5 0 0 0 17.5 8.2a6 6 0 0 0-8.1-4" />
    <path d="M5.5 7.5A4 4 0 0 0 6 17h9" />
    <path d="M2 2l20 20" />
  </svg>
);

export const IconeVoltar = ({ tamanho = 22 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M19 12H5M12 19l-7-7 7-7" />
  </svg>
);

export const IconeMais = ({ tamanho = 20 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M12 5v14M5 12h14" />
  </svg>
);

export const IconeMenos = ({ tamanho = 20 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M5 12h14" />
  </svg>
);

export const IconeFechar = ({ tamanho = 18 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M18 6L6 18M6 6l12 12" />
  </svg>
);

export const IconeBusca = ({ tamanho = 18 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <circle cx="11" cy="11" r="7" />
    <path d="M21 21l-4.3-4.3" />
  </svg>
);

export const IconeEstrela = ({ tamanho = 14, cheia = false }: IconeProps & { cheia?: boolean }) => (
  <svg {...base(tamanho)} fill={cheia ? 'currentColor' : 'none'}>
    <path d="M12 3.2l2.7 5.5 6 .9-4.35 4.24 1.03 6-5.38-2.83L6.6 19.84l1.03-6L3.3 9.6l6-.9L12 3.2z" />
  </svg>
);

export const IconeExpandir = ({ tamanho = 22, aberto = false }: IconeProps & { aberto?: boolean }) => (
  <svg {...base(tamanho)}>
    <path d={aberto ? 'M18 15l-6-6-6 6' : 'M6 9l6 6 6-6'} />
  </svg>
);

export const IconeCama = ({ tamanho = 14 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M3 18v-6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v6" />
    <path d="M3 18h18M7 10V7a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v3" />
  </svg>
);

export const IconeDiaEncerrado = ({ tamanho = 16 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <rect x="3" y="5" width="18" height="16" rx="2" />
    <path d="M16 3v4M8 3v4M3 10h18" />
    <path d="M9 15.5l2 2 4-4" />
  </svg>
);

export const IconeLixeira = ({ tamanho = 16 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M3 6h18" />
    <path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2" />
    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
  </svg>
);

export const IconeMarcarTodos = ({ tamanho = 16 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M2 12.5l4 4L14.5 8" />
    <path d="M11 16.5l1.5 1.5L22 8.5" />
  </svg>
);

export const IconeSortear = ({ tamanho = 16 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M16 3h5v5" />
    <path d="M4 20L21 3" />
    <path d="M21 16v5h-5" />
    <path d="M15 15l6 6M4 4l5 5" />
  </svg>
);

export const IconeInterruptor = ({ tamanho = 20, ligado = false }: IconeProps & { ligado?: boolean }) => (
  <svg {...base(tamanho)}>
    <rect x="1" y="5" width="22" height="14" rx="7" />
    <circle cx={ligado ? 16 : 8} cy="12" r="3" fill="currentColor" stroke="none" />
  </svg>
);

export const IconeEditar = ({ tamanho = 16 }: IconeProps) => (
  <svg {...base(tamanho)}>
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
  </svg>
);

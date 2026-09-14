alter table public.config_financeiro
    add column if not exists pix_tipo text;

update public.config_financeiro
set pix_tipo =
    case
        when pix_chave is null or btrim(pix_chave) = ''      then 'aleatoria'
        when pix_chave like '%@%'                            then 'email'
        when btrim(pix_chave) ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
                                                             then 'aleatoria'
        when length(regexp_replace(pix_chave, '\D', '', 'g')) = 14 then 'cnpj'
        when btrim(pix_chave) like '+%'                      then 'celular'
        when length(regexp_replace(pix_chave, '\D', '', 'g')) in (10, 12, 13) then 'celular'
        else 'aleatoria'
    end
where pix_tipo is null;

alter table public.config_financeiro
    alter column pix_tipo set default 'aleatoria',
    alter column pix_tipo set not null;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'config_financeiro_pix_tipo_valido') then
        alter table public.config_financeiro add constraint config_financeiro_pix_tipo_valido check (
            pix_tipo in ('cpf', 'cnpj', 'celular', 'email', 'aleatoria')
        );
    end if;
end;
$$;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'grupos',
    'grupos',
    true,
    2097152,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update
set public = true,
    file_size_limit = 2097152,
    allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp'];

do $$
declare
    r record;
begin
    for r in
        select policyname
        from pg_policies
        where schemaname = 'storage' and tablename = 'objects' and policyname like 'grupos_logo%'
    loop
        execute format('drop policy %I on storage.objects', r.policyname);
    end loop;
end;
$$;

create policy grupos_logo_leitura_publica on storage.objects
    for select to public
    using (bucket_id = 'grupos');

create policy grupos_logo_envio_diretoria on storage.objects
    for insert to authenticated
    with check (bucket_id = 'grupos' and public.e_diretoria(public.grupo_do_caminho(name)));

create policy grupos_logo_troca_diretoria on storage.objects
    for update to authenticated
    using (bucket_id = 'grupos' and public.e_diretoria(public.grupo_do_caminho(name)))
    with check (bucket_id = 'grupos' and public.e_diretoria(public.grupo_do_caminho(name)));

create policy grupos_logo_exclusao_diretoria on storage.objects
    for delete to authenticated
    using (bucket_id = 'grupos' and public.e_diretoria(public.grupo_do_caminho(name)));

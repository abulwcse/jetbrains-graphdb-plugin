package com.graphdbplugin.datasource

/**
 * Serialisable state container for [DataSourceManager].
 *
 * IntelliJ Platform's [com.intellij.openapi.components.PersistentStateComponent]
 * infrastructure requires the state class to be a plain Java bean — that is, it
 * must have a public no-argument constructor and mutable properties that can be
 * read and written by the XML serialiser ([com.intellij.util.xmlb.XmlSerializer]).
 *
 * This class intentionally has **no business logic**. All CRUD operations are
 * delegated to [DataSourceManager], which owns the state instance and exposes a
 * typed API on top of it.
 *
 * ### Serialisation notes
 * - [dataSources] is serialised as a `<list>` element containing one `<BoltDataSource>`
 *   element per entry, with each field written as a child XML tag.
 * - [BoltDataSource] is a Kotlin `data class` whose properties have default values,
 *   which makes it compatible with XML deserialisation (the deserialiser can construct
 *   an empty instance via reflection and then populate fields individually).
 * - The password field is intentionally absent from [BoltDataSource]; it is stored in
 *   the OS keychain and never touches this XML file.
 *
 * @property dataSources Mutable list of all persisted Bolt data-source configurations.
 *                       Updated by [DataSourceManager] whenever the user adds, edits, or
 *                       deletes a data source.
 */
class DataSourceManagerState {

    /**
     * The ordered list of all data sources the user has configured.
     *
     * The list is initialised as an empty [MutableList] and populated by
     * [DataSourceManager.addDataSource]. The order in the list matches the
     * display order in the tool-window tree.
     */
    var dataSources: MutableList<BoltDataSource> = mutableListOf()
}

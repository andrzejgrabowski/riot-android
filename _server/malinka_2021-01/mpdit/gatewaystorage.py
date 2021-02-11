# ~*~ coding: utf-8 ~*~
""" gateway.py: A starting point for running a gateway between the goTenna network and another external network.
"""

import logging
import sys
import datetime
import threading
import json

try:
    import goTenna
except ImportError:
    # Catch the case where this script is in a sibling directory of the SDK
    sys.path.append('..')
    import goTenna

# auto advetize period in minutes
#AUTO_ADVERTIZE_PER_MIN = 5

#_MODULE_LOGGER = logging.getLogger(__name__)

class GatewayStorage(goTenna.storage.EncryptedFileStorage):
    """ A storage implementation that lets us store our own data alongside the goTenna SDK"""
    def  __init__(self, gotenna_sdk_token):
        """ Initialize the storage.

        :param bytes gotenna_sdk_token: The token for the goTenna SDK.
        """
        goTenna.storage.EncryptedFileStorage.__init__(self, gotenna_sdk_token)

    def set(self, sparse_dict):
        # pylint: disable=line-too-long
        """ Add information to the storage (or overwrite it if present)

        :param dict sparse_dict: A dict that will be added to the internal storage, overwriting everything in its key path if already present
        """
        # pylint: enable=line-too-long
        def _merge(overwrite_with, to_overwrite):
            for key in overwrite_with:
                if key in to_overwrite\
                   and  isinstance(to_overwrite[key], dict)\
                   and isinstance(overwrite_with[key], dict):
                    _merge(overwrite_with[key], to_overwrite[key])
                else:
                    to_overwrite[key] = overwrite_with[key]
        _merge(sparse_dict,  self._cache)

    def get(self, key_path):
        # pylint: disable=line-too-long
        """ Get the results of an arbitrarily long key path

        :param list[str] key_path: A list of keys to traverse in the internal dict. For instance, if the internal dict looks like {'foo': {'bar': 'baz'}}, get(['foo', 'bar']) returns 'baz'.
        :raises KeyError: If ``key_path`` is not in the dict
        """
        # pylint: enable=line-too-long
        if not key_path:
            return self._cache
        def _traverse(key_path, data):
            if len(key_path) == 1:
                # base case: we found our key
                return data[key_path[0]]
            else:
                return  _traverse(key_path[1:], data[key_path[0]])
        return _traverse(key_path, self._cache)

    def remove(self, key_path):
        # pylint: disable=line-too-long
        """ Remove the results of an arbitrarily long key path

        :param list[str] key_path: A list of keys to traverse in the internal path. The leaf node will be deleted. For instance, if the internal dict looks like {'foo': {'bar': 'baz', 'qwx': 'bwz}} and ``key_path`` is ``['foo', 'bar']`` the dict will become ``{'foo': {'bar': 'baz'}}``.
        :raises KeyError: If the ``key_path`` cannot be matched.
        """
        # pylint: enable=line-too-long
        if not key_path:
            return
        def _traverse(key_path, data):
            if len(key_path) == 1:
                del data[key_path[0]]
            else:
                _traverse(key_path[1:], data[key_path[0]])
        _traverse(key_path, self._cache)

    def load(self, gid):
        vals = goTenna.storage.EncryptedFileStorage.load(self, gid)
        self._cache['external_contacts']\
            = {int(ec): ed for ec, ed
               in self._cache.get('external_contacts_ser', {}).items()}
        self._cache['registered_gids']\
            = [goTenna.settings.GID(int(rg),
                                    goTenna.settings.GID.PRIVATE)
               for rg in self._cache.get('registered_gids_ser', [])]
        return vals

    def store(self):
        old_ec = self._cache.pop('external_contacts')
        self._cache['external_contacts_ser']\
            = {str(ec): ed for ec, ed in old_ec.items()}
        old_rg = self._cache.pop('registered_gids')
        self._cache['registered_gids_ser']\
            = [rg.gid_val for rg in old_rg]
        goTenna.storage.EncryptedFileStorage.store(self)
        self._cache['external_contacts'] = old_ec
        self._cache['registered_gids'] = old_rg

